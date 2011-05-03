package org.onebusaway.transit_data_federation.impl.realtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onebusaway.collections.CollectionsLibrary;
import org.onebusaway.collections.FactoryMap;
import org.onebusaway.collections.Min;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data_federation.model.TargetTime;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockVehicleLocationListener;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.realtime.BlockLocation;
import org.onebusaway.transit_data_federation.services.realtime.BlockLocationListener;
import org.onebusaway.transit_data_federation.services.realtime.BlockLocationService;
import org.onebusaway.transit_data_federation.services.realtime.RealTimeHistoryService;
import org.onebusaway.transit_data_federation.services.realtime.ScheduleDeviationSamples;
import org.onebusaway.transit_data_federation.services.realtime.VehicleLocationCacheRecord;
import org.onebusaway.transit_data_federation.services.realtime.VehicleLocationRecordCache;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockStopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

/**
 * Implementation for {@link BlockLocationService}. Keeps a recent cache of
 * {@link BlockLocationRecord} records for current queries and can access
 * database persisted records for queries in the past.
 * 
 * @author bdferris
 * @see BlockLocationService
 */
@Component
@ManagedResource("org.onebusaway.transit_data_federation.impl.realtime:name=BlockLocationServiceImpl")
public class BlockLocationServiceImpl implements BlockLocationService,
    BlockVehicleLocationListener {

  private static Logger _log = LoggerFactory.getLogger(BlockLocationServiceImpl.class);

  private VehicleLocationRecordCache _cache;

  private BlockLocationRecordDao _blockLocationRecordDao;

  private TransitGraphDao _transitGraphDao;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private BlockCalendarService _blockCalendarService;

  private RealTimeHistoryService _realTimeHistoryService;

  private List<BlockLocationListener> _blockLocationListeners = Collections.emptyList();

  /**
   * By default, we keep around 20 minutes of cache entries
   */
  private int _blockLocationRecordCacheWindowSize = 20 * 60;

  private int _predictionCacheMaxOffset = 5 * 60;

  private int _blockInstanceMatchingWindow = 60 * 60 * 1000;

  private boolean _distanceAlongBlockLocationInterpolation = false;

  /**
   * Should block location records be stored to the database?
   */
  private boolean _persistBlockLocationRecords = false;

  /**
   * We queue up block location records so they can be bulk persisted to the
   * database
   */
  private List<BlockLocationRecord> _recordPersistenceQueue = new ArrayList<BlockLocationRecord>();

  /**
   * Used to schedule periodic flushes to the database of the block location
   * records queue
   */
  private ScheduledExecutorService _executor = Executors.newSingleThreadScheduledExecutor();

  /**
   * Block location record persistence stats - last record insert duration
   */
  private volatile long _lastInsertDuration = 0;

  /**
   * Block location record persistence stats - last record insert count
   */
  private volatile long _lastInsertCount = 0;

  /**
   * Records the number of times block location record cache requests fall
   * through to the database
   */
  private AtomicInteger _blockLocationRecordPersistentStoreAccessCount = new AtomicInteger();

  @Autowired
  public void setVehicleLocationRecordCache(VehicleLocationRecordCache cache) {
    _cache = cache;
  }

  @Autowired
  public void setBlockLocationRecordDao(
      BlockLocationRecordDao blockLocationRecordDao) {
    _blockLocationRecordDao = blockLocationRecordDao;
  }

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  @Autowired
  public void setScheduledBlockLocationService(
      ScheduledBlockLocationService scheduleBlockLocationService) {
    _scheduledBlockLocationService = scheduleBlockLocationService;
  }

  @Autowired
  public void setBlockCalendarService(BlockCalendarService blockCalendarService) {
    _blockCalendarService = blockCalendarService;
  }

  @Autowired
  public void setRealTimeHistoryService(
      RealTimeHistoryService realTimeHistoryService) {
    _realTimeHistoryService = realTimeHistoryService;
  }

  @Autowired
  public void setBlockLocationListeners(List<BlockLocationListener> listeners) {
    _blockLocationListeners = listeners;
  }

  /**
   * Controls how far back in time we include records in the
   * {@link BlockLocationRecordCollection} for each active trip.
   * 
   * @param windowSize in seconds
   */
  public void setBlockLocationRecordCacheWindowSize(int windowSize) {
    _blockLocationRecordCacheWindowSize = windowSize;
  }

  /**
   * Should we persist {@link BlockLocationRecord} records to an underlying
   * datastore. Useful if you wish to query trip status for historic analysis.
   * 
   * @param persistTripTimePredictions
   */
  public void setPersistBlockLocationRecords(boolean persistBlockLocationRecords) {
    _persistBlockLocationRecords = persistBlockLocationRecords;
  }

  /****
   * JMX Attributes
   ****/

  @ManagedAttribute
  public long getLastInsertDuration() {
    return _lastInsertDuration;
  }

  @ManagedAttribute
  public long getLastInsertCount() {
    return _lastInsertCount;
  }

  @ManagedAttribute
  public long getBlockLocationRecordPersistentStoreAccessCount() {
    return _blockLocationRecordPersistentStoreAccessCount.get();
  }

  /****
   * Setup and Teardown
   ****/

  @PostConstruct
  public void start() {
    if (_persistBlockLocationRecords)
      _executor.scheduleAtFixedRate(new PredictionWriter(), 0, 1,
          TimeUnit.SECONDS);
  }

  @PreDestroy
  public void stop() {
    _executor.shutdownNow();
  }

  /****
   * {@link BlockVehicleLocationListener} Interface
   ****/

  @Override
  public void handleVehicleLocationRecord(VehicleLocationRecord record) {
    BlockInstance instance = getVehicleLocationRecordAsBlockInstance(record);

    if (instance != null) {

      ScheduledBlockLocation scheduledBlockLocation = getScheduledBlockLocationForVehicleLocationRecord(
          record, instance);

      ScheduleDeviationSamples samples = _realTimeHistoryService.sampleScheduleDeviationsForVehicle(
          instance, record, scheduledBlockLocation);

      putBlockLocationRecord(instance, record, scheduledBlockLocation, samples);
    }
  }

  @Override
  public void resetVehicleLocation(AgencyAndId vehicleId) {
    _cache.clearRecordsForVehicleId(vehicleId);
  }

  /****
   * {@link BlockLocationService} Interface
   ****/

  @Override
  public BlockLocation getLocationForBlockInstance(BlockInstance blockInstance,
      TargetTime time) {

    List<VehicleLocationCacheRecord> records = getBlockLocationRecordCollectionForBlock(
        blockInstance, time);

    VehicleLocationCacheRecord record = null;
    if (!records.isEmpty())
      record = records.get(0);

    // TODO : find a better way to pick?
    return getBlockLocation(blockInstance, record, null, time.getTargetTime());
  }

  @Override
  public BlockLocation getLocationForBlockInstanceAndScheduledBlockLocation(
      BlockInstance blockInstance, ScheduledBlockLocation scheduledLocation,
      long targetTime) {
    return getBlockLocation(blockInstance, null, scheduledLocation, targetTime);
  }

  @Override
  public List<BlockLocation> getLocationsForBlockInstance(
      BlockInstance blockInstance, TargetTime time) {

    List<VehicleLocationCacheRecord> records = getBlockLocationRecordCollectionForBlock(
        blockInstance, time);

    List<BlockLocation> locations = new ArrayList<BlockLocation>();
    for (VehicleLocationCacheRecord cacheRecord : records) {
      BlockLocation location = getBlockLocation(blockInstance, cacheRecord,
          null, time.getTargetTime());
      if (location != null)
        locations.add(location);
    }

    return locations;
  }

  @Override
  public Map<AgencyAndId, List<BlockLocation>> getLocationsForBlockInstance(
      BlockInstance blockInstance, List<Date> times, long currentTime) {

    Map<AgencyAndId, List<BlockLocation>> locationsByVehicleId = new FactoryMap<AgencyAndId, List<BlockLocation>>(
        new ArrayList<BlockLocation>());

    for (Date time : times) {
      TargetTime tt = new TargetTime(time.getTime(), currentTime);
      List<VehicleLocationCacheRecord> records = getBlockLocationRecordCollectionForBlock(
          blockInstance, tt);
      for (VehicleLocationCacheRecord cacheRecord : records) {
        BlockLocation location = getBlockLocation(blockInstance, cacheRecord,
            null, time.getTime());
        if (location != null) {
          locationsByVehicleId.get(location.getVehicleId()).add(location);
        }
      }
    }

    return locationsByVehicleId;
  }

  @Override
  public BlockLocation getScheduledLocationForBlockInstance(
      BlockInstance blockInstance, long targetTime) {
    return getBlockLocation(blockInstance, null, null, targetTime);
  }

  @Override
  public BlockLocation getLocationForVehicleAndTime(AgencyAndId vehicleId,
      TargetTime targetTime) {

    List<VehicleLocationCacheRecord> cacheRecords = getBlockLocationRecordCollectionForVehicle(
        vehicleId, targetTime);

    // TODO : We might take a bit more care in picking the collection if
    // multiple collections are returned
    for (VehicleLocationCacheRecord cacheRecord : cacheRecords) {
      BlockInstance blockInstance = cacheRecord.getBlockInstance();
      BlockLocation location = getBlockLocation(blockInstance, cacheRecord,
          null, targetTime.getTargetTime());
      if (location != null)
        return location;
    }

    return null;
  }

  /****
   * Private Methods
   ****/

  private BlockInstance getVehicleLocationRecordAsBlockInstance(
      VehicleLocationRecord record) {

    AgencyAndId blockId = record.getBlockId();

    if (blockId == null) {
      AgencyAndId tripId = record.getTripId();
      if (tripId == null)
        throw new IllegalArgumentException(
            "at least one of blockId or tripId must be specified for VehicleLocationRecord");
      TripEntry tripEntry = _transitGraphDao.getTripEntryForId(tripId);
      if (tripEntry == null)
        throw new IllegalArgumentException("trip not found with id=" + tripId);
      BlockEntry block = tripEntry.getBlock();
      blockId = block.getId();
    }

    if (record.getServiceDate() == 0)
      throw new IllegalArgumentException("you must specify a serviceDate");

    if (record.getTimeOfRecord() == 0)
      throw new IllegalArgumentException("you must specify a record time");

    BlockInstance blockInstance = getBestBlockForRecord(blockId,
        record.getServiceDate(), record.getTimeOfRecord());

    return blockInstance;
  }

  private BlockInstance getBestBlockForRecord(AgencyAndId blockId,
      long serviceDate, long timeOfRecord) {

    long timeFrom = timeOfRecord - _blockInstanceMatchingWindow;
    long timeTo = timeOfRecord + _blockInstanceMatchingWindow;

    List<BlockInstance> blocks = _blockCalendarService.getActiveBlocks(blockId,
        timeFrom, timeTo);

    if (blocks.isEmpty())
      return null;
    else if (blocks.size() == 1)
      return blocks.get(0);

    Min<BlockInstance> m = new Min<BlockInstance>();

    for (BlockInstance block : blocks) {
      long delta = Math.abs(block.getServiceDate() - serviceDate);
      m.add(delta, block);
    }

    return m.getMinElement();
  }

  /**
   * 
   * @param instance
   * @param record
   * @param scheduledBlockLocation
   */
  private void history(BlockInstance instance, VehicleLocationRecord record,
      ScheduledBlockLocation scheduledBlockLocation) {

  }

  /**
   * We add the {@link BlockPositionRecord} to the local cache and persist it to
   * a back-end data-store if necessary
   * 
   * @param scheduledBlockLocation TODO
   * @param samples
   */
  private void putBlockLocationRecord(BlockInstance blockInstance,
      VehicleLocationRecord record,
      ScheduledBlockLocation scheduledBlockLocation,
      ScheduleDeviationSamples samples) {

    // Cache the result
    _cache.addRecord(blockInstance, record, scheduledBlockLocation, samples);

    if (!CollectionsLibrary.isEmpty(_blockLocationListeners)) {
      BlockLocation location = null;
      for (BlockLocationListener listener : _blockLocationListeners) {
        listener.handleBlockLocation(location);
      }
    }

    if (_persistBlockLocationRecords) {
      List<BlockLocationRecord> blockLocationRecords = getVehicleLocationRecordAsBlockLocationRecord(
          blockInstance, record, scheduledBlockLocation);
      addPredictionToPersistenceQueue(blockLocationRecords);
    }
  }

  /**
   * 
   * @param blockInstance
   * @param scheduledBlockLocation TODO
   * @param targetTime
   * @param record
   * @return null if the effective scheduled block location cannot be determined
   */
  private BlockLocation getBlockLocation(BlockInstance blockInstance,
      VehicleLocationCacheRecord cacheRecord,
      ScheduledBlockLocation scheduledLocation, long targetTime) {

    BlockLocation location = new BlockLocation();
    location.setTime(targetTime);

    location.setBlockInstance(blockInstance);

    if (cacheRecord != null) {

      VehicleLocationRecord record = cacheRecord.getRecord();

      if (scheduledLocation == null)
        scheduledLocation = getScheduledBlockLocationForVehicleLocationCacheRecord(
            cacheRecord, targetTime);

      if (scheduledLocation != null) {
        location.setEffectiveScheduleTime(scheduledLocation.getScheduledTime());
        location.setDistanceAlongBlock(scheduledLocation.getDistanceAlongBlock());

      }

      location.setPredicted(true);
      location.setLastUpdateTime(record.getTimeOfRecord());
      location.setLastLocationUpdateTime(record.getTimeOfLocationUpdate());
      location.setScheduleDeviation(record.getScheduleDeviation());
      location.setScheduleDeviations(cacheRecord.getScheduleDeviations());

      if (record.isCurrentLocationSet()) {
        CoordinatePoint p = new CoordinatePoint(record.getCurrentLocationLat(),
            record.getCurrentLocationLon());
        location.setLastKnownLocation(p);
      }
      location.setOrientation(record.getCurrentOrientation());
      location.setPhase(record.getPhase());
      location.setStatus(record.getStatus());
      location.setVehicleId(record.getVehicleId());

      List<TimepointPredictionRecord> timepointPredictions = record.getTimepointPredictions();
      if (timepointPredictions != null && !timepointPredictions.isEmpty()) {

        SortedMap<Integer, Double> scheduleDeviations = new TreeMap<Integer, Double>();

        BlockConfigurationEntry blockConfig = blockInstance.getBlock();

        for (TimepointPredictionRecord tpr : timepointPredictions) {
          AgencyAndId stopId = tpr.getTimepointId();
          long predictedTime = tpr.getTimepointPredictedTime();
          if (stopId == null || predictedTime == 0)
            continue;

          for (BlockStopTimeEntry blockStopTime : blockConfig.getStopTimes()) {
            StopTimeEntry stopTime = blockStopTime.getStopTime();
            StopEntry stop = stopTime.getStop();
            if (stopId.equals(stop.getId())) {
              int arrivalTime = stopTime.getArrivalTime();
              int deviation = (int) ((tpr.getTimepointPredictedTime() - blockInstance.getServiceDate()) / 1000 - arrivalTime);
              scheduleDeviations.put(arrivalTime, (double) deviation);
            }
          }
        }

        double[] scheduleTimes = new double[scheduleDeviations.size()];
        double[] scheduleDeviationMus = new double[scheduleDeviations.size()];
        double[] scheduleDeviationSigmas = new double[scheduleDeviations.size()];

        int index = 0;
        for (Map.Entry<Integer, Double> entry : scheduleDeviations.entrySet()) {
          scheduleTimes[index] = entry.getKey();
          scheduleDeviationMus[index] = entry.getValue();
          index++;
        }

        ScheduleDeviationSamples samples = new ScheduleDeviationSamples(
            scheduleTimes, scheduleDeviationMus, scheduleDeviationSigmas);
        location.setScheduleDeviations(samples);
      }

    } else {
      if (scheduledLocation == null)
        scheduledLocation = getScheduledBlockLocationForBlockInstance(
            blockInstance, targetTime);
    }

    /**
     * Will be null in the following cases:
     * 
     * 1) When the effective schedule time is beyond the last scheduled stop
     * time for the block.
     * 
     * 2) When the effective distance along block is outside the range of the
     * block's shape.
     */
    if (scheduledLocation == null)
      return null;

    location.setInService(scheduledLocation.isInService());
    location.setActiveTrip(scheduledLocation.getActiveTrip());
    location.setLocation(scheduledLocation.getLocation());
    location.setOrientation(scheduledLocation.getOrientation());
    location.setScheduledDistanceAlongBlock(scheduledLocation.getDistanceAlongBlock());
    location.setClosestStop(scheduledLocation.getClosestStop());
    location.setClosestStopTimeOffset(scheduledLocation.getClosestStopTimeOffset());
    location.setNextStop(scheduledLocation.getNextStop());
    location.setNextStopTimeOffset(scheduledLocation.getNextStopTimeOffset());

    return location;
  }

  /****
   * {@link ScheduledBlockLocation} Methods
   ****/

  private ScheduledBlockLocation getScheduledBlockLocationForVehicleLocationRecord(
      VehicleLocationRecord record, BlockInstance blockInstance) {

    BlockConfigurationEntry blockConfig = blockInstance.getBlock();
    long serviceDate = blockInstance.getServiceDate();

    long targetTime = record.getTimeOfRecord();

    int scheduledTime = (int) ((targetTime - serviceDate) / 1000);

    if (record.isScheduleDeviationSet()) {

      /**
       * Effective scheduled time is the point that a transit vehicle is at on
       * its schedule, with schedule deviation taken into account. So if it's
       * 100 minutes into the current service date and the bus is running 10
       * minutes late, it's actually at the 90 minute point in its scheduled
       * operation.
       */
      int effectiveScheduledTime = (int) (scheduledTime - record.getScheduleDeviation());

      return _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
          blockConfig, effectiveScheduledTime);
    }

    if (record.isDistanceAlongBlockSet()) {
      return _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
          blockConfig, record.getDistanceAlongBlock());
    }

    return _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
        blockConfig, scheduledTime);
  }

  private ScheduledBlockLocation getScheduledBlockLocationForBlockInstance(
      BlockInstance blockInstance, long targetTime) {

    BlockConfigurationEntry blockConfig = blockInstance.getBlock();
    long serviceDate = blockInstance.getServiceDate();

    int scheduledTime = (int) ((targetTime - serviceDate) / 1000);

    return _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
        blockConfig, scheduledTime);
  }

  private ScheduledBlockLocation getScheduledBlockLocationForVehicleLocationCacheRecord(
      VehicleLocationCacheRecord cacheRecord, long targetTime) {

    BlockInstance blockInstance = cacheRecord.getBlockInstance();
    VehicleLocationRecord record = cacheRecord.getRecord();
    ScheduledBlockLocation scheduledBlockLocation = cacheRecord.getScheduledBlockLocation();

    BlockConfigurationEntry blockConfig = blockInstance.getBlock();
    long serviceDate = blockInstance.getServiceDate();

    int scheduledTime = (int) ((targetTime - serviceDate) / 1000);

    if (record.isScheduleDeviationSet()) {

      /**
       * Effective scheduled time is the point that a transit vehicle is at on
       * its schedule, with schedule deviation taken into account. So if it's
       * 100 minutes into the current service date and the bus is running 10
       * minutes late, it's actually at the 90 minute point in its scheduled
       * operation.
       */
      int effectiveScheduledTime = (int) (scheduledTime - record.getScheduleDeviation());

      if (scheduledBlockLocation != null
          && scheduledBlockLocation.getScheduledTime() <= effectiveScheduledTime) {

        return _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
            scheduledBlockLocation, effectiveScheduledTime);
      }

      return _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
          blockConfig, effectiveScheduledTime);
    }

    if (record.isDistanceAlongBlockSet()) {

      if (_distanceAlongBlockLocationInterpolation
          && scheduledBlockLocation != null
          && scheduledBlockLocation.getDistanceAlongBlock() <= record.getDistanceAlongBlock()) {

        int ellapsedTime = (int) ((targetTime - record.getTimeOfRecord()) / 1000);

        if (ellapsedTime >= 0) {

          int effectiveScheduledTime = scheduledBlockLocation.getScheduledTime()
              + ellapsedTime;

          return _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
              blockConfig, effectiveScheduledTime);
        }

        return _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
            scheduledBlockLocation, record.getDistanceAlongBlock());
      }

      return _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
          blockConfig, record.getDistanceAlongBlock());
    }

    return _scheduledBlockLocationService.getScheduledBlockLocationFromScheduledTime(
        blockConfig, scheduledTime);
  }

  private List<VehicleLocationCacheRecord> getBlockLocationRecordCollectionForBlock(
      BlockInstance blockInstance, TargetTime time) {
    return getBlockLocationRecordCollections(new BlockInstanceStrategy(
        blockInstance), time);
  }

  private List<VehicleLocationCacheRecord> getBlockLocationRecordCollectionForVehicle(
      AgencyAndId vehicleId, TargetTime time) {
    return getBlockLocationRecordCollections(new VehicleIdRecordStrategy(
        vehicleId), time);
  }

  private List<VehicleLocationCacheRecord> getBlockLocationRecordCollections(
      RecordStrategy strategy, TargetTime time) {

    List<VehicleLocationCacheRecord> records = strategy.getRecordsFromCache();

    if (!records.isEmpty()) {
      List<VehicleLocationCacheRecord> inRange = new ArrayList<VehicleLocationCacheRecord>();
      long offset = _predictionCacheMaxOffset * 1000;
      for (VehicleLocationCacheRecord entry : records) {
        VehicleLocationRecord record = entry.getRecord();
        if (record.getTimeOfRecord() - offset <= time.getCurrentTime()
            && time.getCurrentTime() <= record.getTimeOfRecord() + offset)
          inRange.add(entry);
      }
      if (!inRange.isEmpty())
        return inRange;
    }

    long offset = _blockLocationRecordCacheWindowSize * 1000 / 2;

    // We only consult persisted cache entries if the requested target time is
    // not within our current cache window
    boolean outOfRange = time.getTargetTime() + offset < time.getCurrentTime()
        || time.getCurrentTime() < time.getTargetTime() - offset;

    if (outOfRange && _persistBlockLocationRecords) {

      _blockLocationRecordPersistentStoreAccessCount.incrementAndGet();

      long fromTime = time.getTargetTime() - offset;
      long toTime = time.getTargetTime() + offset;

      List<BlockLocationRecord> predictions = strategy.getRecordsFromDao(
          fromTime, toTime);

      if (!predictions.isEmpty()) {

        Map<BlockLocationRecordKey, List<BlockLocationRecord>> recordsByKey = groupRecord(predictions);

        List<VehicleLocationCacheRecord> allCollections = new ArrayList<VehicleLocationCacheRecord>();
        for (Map.Entry<BlockLocationRecordKey, List<BlockLocationRecord>> entry : recordsByKey.entrySet()) {
          BlockLocationRecordKey key = entry.getKey();
          List<BlockLocationRecord> blockLocationRecords = entry.getValue();
          List<VehicleLocationCacheRecord> someRecords = getBlockLocationRecordsAsVehicleLocationRecords(
              key.getBlockInstance(), blockLocationRecords);
          allCollections.addAll(someRecords);
        }

        return allCollections;
      }
    }

    return Collections.emptyList();
  }

  private List<BlockLocationRecord> getVehicleLocationRecordAsBlockLocationRecord(
      BlockInstance blockInstance, VehicleLocationRecord record,
      ScheduledBlockLocation scheduledBlockLocation) {

    BlockLocationRecord.Builder builder = BlockLocationRecord.builder();

    if (scheduledBlockLocation != null) {

      BlockTripEntry activeTrip = scheduledBlockLocation.getActiveTrip();
      builder.setTripId(activeTrip.getTrip().getId());
      builder.setBlockId(activeTrip.getBlockConfiguration().getBlock().getId());

      double distanceAlongBlock = scheduledBlockLocation.getDistanceAlongBlock();
      builder.setDistanceAlongBlock(distanceAlongBlock);

      double distanceAlongTrip = distanceAlongBlock
          - activeTrip.getDistanceAlongBlock();
      builder.setDistanceAlongTrip(distanceAlongTrip);
    }

    if (record.getBlockId() != null)
      builder.setBlockId(record.getBlockId());
    if (record.getTripId() != null)
      builder.setTripId(record.getTripId());
    builder.setTime(record.getTimeOfRecord());
    builder.setServiceDate(record.getServiceDate());

    if (record.isScheduleDeviationSet())
      builder.setScheduleDeviation(record.getScheduleDeviation());

    if (record.isDistanceAlongBlockSet()) {
      double distanceAlongBlock = record.getDistanceAlongBlock();
      builder.setDistanceAlongBlock(distanceAlongBlock);
      AgencyAndId tripId = record.getTripId();
      if (tripId != null) {
        BlockConfigurationEntry block = blockInstance.getBlock();
        for (BlockTripEntry blockTrip : block.getTrips()) {
          TripEntry trip = blockTrip.getTrip();
          if (trip.getId().equals(tripId)) {
            double distanceAlongTrip = distanceAlongBlock
                - blockTrip.getDistanceAlongBlock();
            builder.setDistanceAlongTrip(distanceAlongTrip);
          }
        }
      }
    }

    if (record.isCurrentLocationSet()) {
      builder.setLocationLat(record.getCurrentLocationLat());
      builder.setLocationLon(record.getCurrentLocationLon());
    }

    if (record.isCurrentOrientationSet())
      builder.setOrientation(record.getCurrentOrientation());

    builder.setPhase(record.getPhase());
    builder.setStatus(record.getStatus());
    builder.setVehicleId(record.getVehicleId());

    List<TimepointPredictionRecord> predictions = record.getTimepointPredictions();
    if (predictions == null || predictions.isEmpty())
      return Arrays.asList(builder.create());

    List<BlockLocationRecord> results = new ArrayList<BlockLocationRecord>();
    for (TimepointPredictionRecord tpr : predictions) {
      builder.setTimepointId(tpr.getTimepointId());
      builder.setTimepointScheduledTime(tpr.getTimepointScheduledTime());
      builder.setTimepointPredictedTime(tpr.getTimepointPredictedTime());
      results.add(builder.create());
    }
    return results;
  }

  private List<VehicleLocationCacheRecord> getBlockLocationRecordsAsVehicleLocationRecords(
      BlockInstance blockInstance, List<BlockLocationRecord> records) {

    List<VehicleLocationCacheRecord> results = new ArrayList<VehicleLocationCacheRecord>();

    for (BlockLocationRecord record : records) {
      VehicleLocationRecord vlr = new VehicleLocationRecord();
      vlr.setBlockId(blockInstance.getBlock().getBlock().getId());
      if (record.isLocationSet()) {
        vlr.setCurrentLocationLat(record.getLocationLat());
        vlr.setCurrentLocationLon(record.getLocationLon());
      }
      if (record.isOrientationSet())
        vlr.setCurrentOrientation(record.getOrientation());
      if (record.isDistanceAlongBlockSet())
        vlr.setDistanceAlongBlock(record.getDistanceAlongBlock());
      vlr.setPhase(record.getPhase());
      if (record.isScheduleDeviationSet())
        vlr.setScheduleDeviation(record.getScheduleDeviation());
      vlr.setServiceDate(record.getServiceDate());
      vlr.setStatus(record.getStatus());
      vlr.setTimeOfRecord(record.getTime());
      vlr.setVehicleId(record.getVehicleId());

      VehicleLocationCacheRecord cacheRecord = new VehicleLocationCacheRecord(
          blockInstance, vlr, null, null);
      results.add(cacheRecord);
    }

    return results;
  }

  private Map<BlockLocationRecordKey, List<BlockLocationRecord>> groupRecord(
      List<BlockLocationRecord> predictions) {

    Map<BlockLocationRecordKey, List<BlockLocationRecord>> recordsByKey = new FactoryMap<BlockLocationRecordKey, List<BlockLocationRecord>>(
        new ArrayList<BlockLocationRecord>());

    for (BlockLocationRecord record : predictions) {
      AgencyAndId blockId = record.getBlockId();
      long serviceDate = record.getServiceDate();
      BlockInstance blockInstance = _blockCalendarService.getBlockInstance(
          blockId, serviceDate);
      if (blockInstance != null) {
        BlockLocationRecordKey key = new BlockLocationRecordKey(blockInstance,
            record.getVehicleId());
        recordsByKey.get(key).add(record);
      }
    }

    return recordsByKey;
  }

  private void addPredictionToPersistenceQueue(List<BlockLocationRecord> records) {
    synchronized (_recordPersistenceQueue) {
      _recordPersistenceQueue.addAll(records);
    }
  }

  private List<BlockLocationRecord> getPredictionPersistenceQueue() {
    synchronized (_recordPersistenceQueue) {
      List<BlockLocationRecord> queue = new ArrayList<BlockLocationRecord>(
          _recordPersistenceQueue);
      _recordPersistenceQueue.clear();
      return queue;
    }
  }

  private class PredictionWriter implements Runnable {

    @Override
    public void run() {

      try {
        List<BlockLocationRecord> queue = getPredictionPersistenceQueue();

        if (queue.isEmpty())
          return;

        long t1 = System.currentTimeMillis();
        _blockLocationRecordDao.saveBlockLocationRecords(queue);
        long t2 = System.currentTimeMillis();
        _lastInsertDuration = t2 - t1;
        _lastInsertCount = queue.size();
      } catch (Throwable ex) {
        _log.error("error writing block location records to dao", ex);
      }
    }
  }

  private interface RecordStrategy {

    public List<VehicleLocationCacheRecord> getRecordsFromCache();

    public List<BlockLocationRecord> getRecordsFromDao(long fromTime,
        long toTime);
  }

  private class BlockInstanceStrategy implements RecordStrategy {

    private BlockInstance _blockInstance;

    public BlockInstanceStrategy(BlockInstance blockInstance) {
      _blockInstance = blockInstance;
    }

    @Override
    public List<VehicleLocationCacheRecord> getRecordsFromCache() {
      return _cache.getRecordsForBlockInstance(_blockInstance);
    }

    @Override
    public List<BlockLocationRecord> getRecordsFromDao(long fromTime,
        long toTime) {
      BlockConfigurationEntry blockConfig = _blockInstance.getBlock();
      BlockEntry block = blockConfig.getBlock();
      return _blockLocationRecordDao.getBlockLocationRecordsForBlockServiceDateAndTimeRange(
          block.getId(), _blockInstance.getServiceDate(), fromTime, toTime);
    }
  }

  private class VehicleIdRecordStrategy implements RecordStrategy {

    private AgencyAndId _vehicleId;

    public VehicleIdRecordStrategy(AgencyAndId vehicleId) {
      _vehicleId = vehicleId;
    }

    public List<VehicleLocationCacheRecord> getRecordsFromCache() {
      VehicleLocationCacheRecord recordForVehicleId = _cache.getRecordForVehicleId(_vehicleId);
      if (recordForVehicleId == null)
        return Collections.emptyList();
      return Arrays.asList(recordForVehicleId);
    }

    public List<BlockLocationRecord> getRecordsFromDao(long fromTime,
        long toTime) {
      return _blockLocationRecordDao.getBlockLocationRecordsForVehicleAndTimeRange(
          _vehicleId, fromTime, toTime);
    }
  }
}
