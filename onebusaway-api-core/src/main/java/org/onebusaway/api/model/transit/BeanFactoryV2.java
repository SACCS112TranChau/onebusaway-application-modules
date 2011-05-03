package org.onebusaway.api.model.transit;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.api.impl.MaxCountSupport;
import org.onebusaway.api.model.transit.blocks.BlockConfigurationV2Bean;
import org.onebusaway.api.model.transit.blocks.BlockInstanceV2Bean;
import org.onebusaway.api.model.transit.blocks.BlockStopTimeV2Bean;
import org.onebusaway.api.model.transit.blocks.BlockTripV2Bean;
import org.onebusaway.api.model.transit.blocks.BlockV2Bean;
import org.onebusaway.api.model.transit.realtime.CurrentVehicleEstimateV2Bean;
import org.onebusaway.api.model.transit.schedule.StopTimeV2Bean;
import org.onebusaway.api.model.transit.service_alerts.NaturalLanguageStringV2Bean;
import org.onebusaway.api.model.transit.service_alerts.SituationAffectedAgencyV2Bean;
import org.onebusaway.api.model.transit.service_alerts.SituationAffectedApplicationV2Bean;
import org.onebusaway.api.model.transit.service_alerts.SituationAffectedCallV2Bean;
import org.onebusaway.api.model.transit.service_alerts.SituationAffectedStopV2Bean;
import org.onebusaway.api.model.transit.service_alerts.SituationAffectedVehicleJourneyV2Bean;
import org.onebusaway.api.model.transit.service_alerts.SituationAffectsV2Bean;
import org.onebusaway.api.model.transit.service_alerts.SituationConditionDetailsV2Bean;
import org.onebusaway.api.model.transit.service_alerts.SituationConsequenceV2Bean;
import org.onebusaway.api.model.transit.service_alerts.SituationV2Bean;
import org.onebusaway.api.model.transit.service_alerts.TimeRangeV2Bean;
import org.onebusaway.api.model.transit.tripplanning.MinTravelTimeToStopV2Bean;
import org.onebusaway.collections.CollectionsLibrary;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.transit_data.model.AgencyBean;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RoutesBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopCalendarDayBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopRouteDirectionScheduleBean;
import org.onebusaway.transit_data.model.StopRouteScheduleBean;
import org.onebusaway.transit_data.model.StopScheduleBean;
import org.onebusaway.transit_data.model.StopTimeInstanceBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.TimeIntervalBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.TripStopTimesBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockBean;
import org.onebusaway.transit_data.model.blocks.BlockConfigurationBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockStopTimeBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.oba.MinTravelTimeToStopsBean;
import org.onebusaway.transit_data.model.realtime.CurrentVehicleEstimateBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.schedule.FrequencyBean;
import org.onebusaway.transit_data.model.schedule.FrequencyInstanceBean;
import org.onebusaway.transit_data.model.schedule.StopTimeBean;
import org.onebusaway.transit_data.model.service_alerts.ESensitivity;
import org.onebusaway.transit_data.model.service_alerts.ESeverity;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectedAgencyBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectedApplicationBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectedCallBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectedStopBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectedVehicleJourneyBean;
import org.onebusaway.transit_data.model.service_alerts.SituationAffectsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationBean;
import org.onebusaway.transit_data.model.service_alerts.SituationConditionDetailsBean;
import org.onebusaway.transit_data.model.service_alerts.SituationConsequenceBean;
import org.onebusaway.transit_data.model.service_alerts.TimeRangeBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

public class BeanFactoryV2 {

  private boolean _includeReferences = true;

  private boolean _includeConditionDetails = true;

  private ReferencesBean _references = new ReferencesBean();

  private MaxCountSupport _maxCount;

  private String _applicationKey;

  public BeanFactoryV2(boolean includeReferences) {
    _includeReferences = includeReferences;
  }

  public void setIncludeConditionDetails(boolean includeConditionDetails) {
    _includeConditionDetails = includeConditionDetails;
  }

  public void setMaxCount(MaxCountSupport maxCount) {
    _maxCount = maxCount;
  }

  public void setApplicationKey(String applicationKey) {
    _applicationKey = applicationKey;
  }

  /****
   * Response Methods
   ****/

  public EntryWithReferencesBean<AgencyV2Bean> getResponse(AgencyBean agency) {
    return entry(getAgency(agency));
  }

  public EntryWithReferencesBean<RouteV2Bean> getResponse(RouteBean route) {
    return entry(getRoute(route));
  }

  public EntryWithReferencesBean<EncodedPolylineBean> getResponse(
      EncodedPolylineBean bean) {
    return entry(bean);
  }

  public Object getResponse(StopBean stop) {
    return entry(getStop(stop));
  }

  public EntryWithReferencesBean<TripV2Bean> getResponse(TripBean trip) {
    return entry(getTrip(trip));
  }

  public EntryWithReferencesBean<TripDetailsV2Bean> getResponse(
      TripDetailsBean tripDetails) {
    return entry(getTripDetails(tripDetails));
  }

  public EntryWithReferencesBean<BlockV2Bean> getBlockResponse(BlockBean block) {
    return entry(getBlock(block));
  }

  public EntryWithReferencesBean<StopWithArrivalsAndDeparturesV2Bean> getResponse(
      StopWithArrivalsAndDeparturesBean result) {
    return entry(getStopWithArrivalAndDepartures(result));
  }

  public EntryWithReferencesBean<ArrivalAndDepartureV2Bean> getResponse(
      ArrivalAndDepartureBean result) {
    return entry(getArrivalAndDeparture(result));
  }

  public EntryWithReferencesBean<StopScheduleV2Bean> getResponse(
      StopScheduleBean stopSchedule) {
    return entry(getStopSchedule(stopSchedule));
  }

  public EntryWithReferencesBean<StopsForRouteV2Bean> getResponse(
      StopsForRouteBean result, boolean includePolylines) {
    return entry(getStopsForRoute(result, includePolylines));
  }

  public ListWithReferencesBean<AgencyWithCoverageV2Bean> getResponse(
      List<AgencyWithCoverageBean> beans) {
    List<AgencyWithCoverageV2Bean> list = new ArrayList<AgencyWithCoverageV2Bean>();
    for (AgencyWithCoverageBean bean : filter(beans))
      list.add(getAgencyWithCoverage(bean));
    return list(list, list.size() < beans.size());
  }

  public ListWithReferencesBean<RouteV2Bean> getResponse(RoutesBean result) {
    List<RouteV2Bean> beans = new ArrayList<RouteV2Bean>();
    for (RouteBean route : result.getRoutes())
      beans.add(getRoute(route));
    return list(beans, result.isLimitExceeded(), false);
  }

  public ListWithReferencesBean<StopV2Bean> getResponse(StopsBean result) {
    List<StopV2Bean> beans = new ArrayList<StopV2Bean>();
    for (StopBean stop : result.getStops())
      beans.add(getStop(stop));
    return list(beans, result.isLimitExceeded(), false);
  }

  public ListWithReferencesBean<TripDetailsV2Bean> getTripDetailsResponse(
      ListBean<TripDetailsBean> trips) {

    List<TripDetailsV2Bean> beans = new ArrayList<TripDetailsV2Bean>();
    for (TripDetailsBean trip : trips.getList())
      beans.add(getTripDetails(trip));
    return list(beans, trips.isLimitExceeded(), false);
  }

  public ListWithReferencesBean<VehicleStatusV2Bean> getVehicleStatusResponse(
      ListBean<VehicleStatusBean> vehicles) {

    List<VehicleStatusV2Bean> beans = new ArrayList<VehicleStatusV2Bean>();
    for (VehicleStatusBean vehicle : vehicles.getList())
      beans.add(getVehicleStatus(vehicle));
    return list(beans, vehicles.isLimitExceeded(), false);
  }

  public ListWithReferencesBean<VehicleLocationRecordV2Bean> getVehicleLocationRecordResponse(
      ListBean<VehicleLocationRecordBean> vehicles) {

    List<VehicleLocationRecordV2Bean> beans = new ArrayList<VehicleLocationRecordV2Bean>();
    for (VehicleLocationRecordBean vehicle : vehicles.getList())
      beans.add(getVehicleLocationRecord(vehicle));
    return list(beans, vehicles.isLimitExceeded(), false);
  }

  public EntryWithReferencesBean<VehicleStatusV2Bean> getVehicleStatusResponse(
      VehicleStatusBean vehicleStatus) {
    return entry(getVehicleStatus(vehicleStatus));
  }

  public EntryWithReferencesBean<SituationV2Bean> getResponse(
      SituationBean situation) {
    return entry(getSituation(situation));
  }

  public ListWithReferencesBean<MinTravelTimeToStopV2Bean> getMinTravelTimeToStops(
      MinTravelTimeToStopsBean travelTimes) {
    List<MinTravelTimeToStopV2Bean> beans = new ArrayList<MinTravelTimeToStopV2Bean>();
    for (int i = 0; i < travelTimes.getSize(); i++) {
      MinTravelTimeToStopV2Bean bean = new MinTravelTimeToStopV2Bean();
      bean.setStopId(travelTimes.getStopId(i));
      bean.setLocation(new CoordinatePoint(travelTimes.getStopLat(i),
          travelTimes.getStopLon(i)));
      bean.setTravelTime(travelTimes.getTravelTime(i));
      beans.add(bean);
    }
    return list(beans, false);
  }

  /****
   * 
   *****/

  public ListWithReferencesBean<String> getEntityIdsResponse(
      ListBean<String> ids) {
    return list(ids.getList(), ids.isLimitExceeded());
  }

  public <T> ListWithReferencesBean<T> getEmptyList(Class<T> type,
      boolean outOfRange) {
    return list(new ArrayList<T>(), false, outOfRange);
  }

  /****
   * 
   ***/

  public TimeIntervalV2 getTimeInterval(TimeIntervalBean interval) {
    if (interval == null)
      return null;
    TimeIntervalV2 bean = new TimeIntervalV2();
    bean.setFrom(interval.getFrom());
    bean.setTo(interval.getTo());
    return bean;
  }

  public AgencyV2Bean getAgency(AgencyBean agency) {
    AgencyV2Bean bean = new AgencyV2Bean();
    bean.setDisclaimer(agency.getDisclaimer());
    bean.setId(agency.getId());
    bean.setLang(agency.getLang());
    bean.setName(agency.getName());
    bean.setPhone(agency.getPhone());
    bean.setTimezone(agency.getTimezone());
    bean.setUrl(agency.getUrl());
    return bean;
  }

  public RouteV2Bean getRoute(RouteBean route) {
    RouteV2Bean bean = new RouteV2Bean();

    bean.setAgencyId(route.getAgency().getId());
    addToReferences(route.getAgency());

    bean.setColor(route.getColor());
    bean.setDescription(route.getDescription());
    bean.setId(route.getId());
    bean.setLongName(route.getLongName());
    bean.setShortName(route.getShortName());
    bean.setTextColor(route.getTextColor());
    bean.setType(route.getType());
    bean.setUrl(route.getUrl());

    return bean;
  }

  public StopV2Bean getStop(StopBean stop) {
    StopV2Bean bean = new StopV2Bean();
    bean.setCode(stop.getCode());
    bean.setDirection(stop.getDirection());
    bean.setId(stop.getId());
    bean.setLat(stop.getLat());
    bean.setLon(stop.getLon());
    bean.setLocationType(stop.getLocationType());
    bean.setName(stop.getName());

    List<String> routeIds = new ArrayList<String>();
    for (RouteBean route : stop.getRoutes()) {
      routeIds.add(route.getId());
      addToReferences(route);
    }
    bean.setRouteIds(routeIds);

    return bean;
  }

  public TripV2Bean getTrip(TripBean trip) {

    TripV2Bean bean = new TripV2Bean();

    bean.setId(trip.getId());

    bean.setRouteId(trip.getRoute().getId());
    addToReferences(trip.getRoute());

    bean.setRouteShortName(trip.getRouteShortName());
    bean.setTripHeadsign(trip.getTripHeadsign());
    bean.setTripShortName(trip.getTripShortName());

    bean.setDirectionId(trip.getDirectionId());
    bean.setServiceId(trip.getServiceId());
    bean.setShapeId(trip.getShapeId());
    bean.setBlockId(trip.getBlockId());

    return bean;
  }

  public TripStatusV2Bean getTripStatus(TripStatusBean tripStatus) {

    TripStatusV2Bean bean = new TripStatusV2Bean();

    TripBean activeTrip = tripStatus.getActiveTrip();
    if (activeTrip != null) {
      bean.setActiveTripId(activeTrip.getId());
      bean.setBlockTripSequence(tripStatus.getBlockTripSequence());
      addToReferences(activeTrip);
    }

    bean.setServiceDate(tripStatus.getServiceDate());

    FrequencyBean frequency = tripStatus.getFrequency();
    if (frequency != null)
      bean.setFrequency(getFrequency(frequency));

    bean.setScheduledDistanceAlongTrip(tripStatus.getScheduledDistanceAlongTrip());
    bean.setTotalDistanceAlongTrip(tripStatus.getTotalDistanceAlongTrip());

    bean.setPosition(tripStatus.getLocation());
    if (tripStatus.isOrientationSet())
      bean.setOrientation(tripStatus.getOrientation());

    StopBean closestStop = tripStatus.getClosestStop();
    if (closestStop != null) {
      bean.setClosestStop(closestStop.getId());
      addToReferences(closestStop);
      bean.setClosestStopTimeOffset(tripStatus.getClosestStopTimeOffset());
    }

    StopBean nextStop = tripStatus.getNextStop();
    if (nextStop != null) {
      bean.setNextStop(nextStop.getId());
      addToReferences(nextStop);
      bean.setNextStopTimeOffset(tripStatus.getNextStopTimeOffset());
    }

    bean.setPhase(tripStatus.getPhase());
    bean.setStatus(tripStatus.getStatus());

    bean.setPredicted(tripStatus.isPredicted());

    if (tripStatus.getLastUpdateTime() > 0)
      bean.setLastUpdateTime(tripStatus.getLastUpdateTime());

    if (tripStatus.getLastLocationUpdateTime() > 0)
      bean.setLastLocationUpdateTime(tripStatus.getLastLocationUpdateTime());

    if (tripStatus.isLastKnownDistanceAlongTripSet())
      bean.setLastKnownDistanceAlongTrip(tripStatus.getLastKnownDistanceAlongTrip());

    bean.setLastKnownLocation(tripStatus.getLastKnownLocation());

    if (tripStatus.isLastKnownOrientationSet())
      bean.setLastKnownOrientation(tripStatus.getLastKnownOrientation());

    if (tripStatus.isScheduleDeviationSet())
      bean.setScheduleDeviation((int) tripStatus.getScheduleDeviation());
    if (tripStatus.isDistanceAlongTripSet())
      bean.setDistanceAlongTrip(tripStatus.getDistanceAlongTrip());
    bean.setVehicleId(tripStatus.getVehicleId());

    List<SituationBean> situations = tripStatus.getSituations();
    if (situations != null && !situations.isEmpty()) {
      List<String> situationIds = new ArrayList<String>();
      for (SituationBean situation : situations) {
        situationIds.add(situation.getId());
        addToReferences(situation);
      }
      bean.setSituationIds(situationIds);
    }

    return bean;
  }

  public TripStopTimesV2Bean getTripStopTimes(TripStopTimesBean tripStopTimes) {

    TripStopTimesV2Bean bean = new TripStopTimesV2Bean();

    bean.setTimeZone(tripStopTimes.getTimeZone());

    List<TripStopTimeV2Bean> instances = new ArrayList<TripStopTimeV2Bean>();
    for (TripStopTimeBean sti : tripStopTimes.getStopTimes()) {

      TripStopTimeV2Bean stiBean = new TripStopTimeV2Bean();
      stiBean.setArrivalTime(sti.getArrivalTime());
      stiBean.setDepartureTime(sti.getDepartureTime());
      stiBean.setStopHeadsign(sti.getStopHeadsign());
      stiBean.setDistanceAlongTrip(sti.getDistanceAlongTrip());

      stiBean.setStopId(sti.getStop().getId());
      addToReferences(sti.getStop());

      instances.add(stiBean);
    }

    bean.setStopTimes(instances);

    TripBean nextTrip = tripStopTimes.getNextTrip();
    if (nextTrip != null) {
      bean.setNextTripId(nextTrip.getId());
      addToReferences(nextTrip);
    }

    TripBean prevTrip = tripStopTimes.getPreviousTrip();
    if (prevTrip != null) {
      bean.setPreviousTripId(prevTrip.getId());
      addToReferences(prevTrip);
    }

    FrequencyBean freq = tripStopTimes.getFrequency();
    if (freq != null)
      bean.setFrequency(getFrequency(freq));

    return bean;
  }

  public TripDetailsV2Bean getTripDetails(TripDetailsBean tripDetails) {

    TripDetailsV2Bean bean = new TripDetailsV2Bean();

    bean.setTripId(tripDetails.getTripId());
    bean.setServiceDate(tripDetails.getServiceDate());

    if (tripDetails.getFrequency() != null)
      bean.setFrequency(getFrequency(tripDetails.getFrequency()));

    TripBean trip = tripDetails.getTrip();
    if (trip != null)
      addToReferences(trip);

    TripStopTimesBean stopTimes = tripDetails.getSchedule();
    if (stopTimes != null)
      bean.setSchedule(getTripStopTimes(stopTimes));

    TripStatusBean status = tripDetails.getStatus();
    if (status != null)
      bean.setStatus(getTripStatus(status));

    List<SituationBean> situations = tripDetails.getSituations();
    if (!CollectionsLibrary.isEmpty(situations)) {
      List<String> situationIds = new ArrayList<String>();
      for (SituationBean situation : situations) {
        addToReferences(situation);
        situationIds.add(situation.getId());
      }
      bean.setSituationIds(situationIds);
    }

    return bean;
  }

  public BlockInstanceV2Bean getBlockInstance(BlockInstanceBean blockInstance) {
    BlockInstanceV2Bean bean = new BlockInstanceV2Bean();
    bean.setBlockConfiguration(getBlockConfig(blockInstance.getBlockConfiguration()));
    bean.setBlockId(blockInstance.getBlockId());
    if (blockInstance.getFrequency() != null)
      bean.setFrequency(getFrequency(blockInstance.getFrequency()));
    bean.setServiceDate(blockInstance.getServiceDate());
    return bean;
  }

  public BlockV2Bean getBlock(BlockBean block) {
    BlockV2Bean bean = new BlockV2Bean();
    bean.setId(block.getId());
    List<BlockConfigurationV2Bean> blockConfigs = new ArrayList<BlockConfigurationV2Bean>();
    for (BlockConfigurationBean blockConfig : block.getConfigurations())
      blockConfigs.add(getBlockConfig(blockConfig));
    bean.setConfigurations(blockConfigs);
    return bean;
  }

  public BlockConfigurationV2Bean getBlockConfig(
      BlockConfigurationBean blockConfig) {
    BlockConfigurationV2Bean bean = new BlockConfigurationV2Bean();
    bean.setActiveServiceIds(blockConfig.getActiveServiceIds());
    bean.setInactiveServiceIds(blockConfig.getInactiveServiceIds());
    List<BlockTripV2Bean> blockTrips = new ArrayList<BlockTripV2Bean>();
    for (BlockTripBean blockTrip : blockConfig.getTrips())
      blockTrips.add(getBlockTrip(blockTrip));
    bean.setTrips(blockTrips);
    return bean;
  }

  public BlockTripV2Bean getBlockTrip(BlockTripBean blockTrip) {

    BlockTripV2Bean bean = new BlockTripV2Bean();
    bean.setAccumulatedSlackTime(blockTrip.getAccumulatedSlackTime());
    bean.setDistanceAlongBlock(blockTrip.getDistanceAlongBlock());

    addToReferences(blockTrip.getTrip());
    bean.setTripId(blockTrip.getTrip().getId());

    List<BlockStopTimeV2Bean> blockStopTimes = new ArrayList<BlockStopTimeV2Bean>();
    for (BlockStopTimeBean blockStopTime : blockTrip.getBlockStopTimes()) {
      BlockStopTimeV2Bean stopTimeBean = getBlockStopTime(blockStopTime);
      blockStopTimes.add(stopTimeBean);
    }
    bean.setBlockStopTimes(blockStopTimes);

    return bean;
  }

  public BlockStopTimeV2Bean getBlockStopTime(BlockStopTimeBean blockStopTime) {
    BlockStopTimeV2Bean bean = new BlockStopTimeV2Bean();
    bean.setAccumulatedSlackTime(blockStopTime.getAccumulatedSlackTime());
    bean.setBlockSequence(blockStopTime.getBlockSequence());
    bean.setDistanceAlongBlock(blockStopTime.getDistanceAlongBlock());
    bean.setStopTime(getStopTime(blockStopTime.getStopTime()));
    return bean;
  }

  public StopTimeV2Bean getStopTime(StopTimeBean stopTime) {
    StopTimeV2Bean bean = new StopTimeV2Bean();
    bean.setArrivalTime(stopTime.getArrivalTime());
    bean.setDepartureTime(stopTime.getDepartureTime());
    bean.setDropOffType(stopTime.getDropOffType());
    bean.setPickupType(stopTime.getPickupType());

    bean.setStopId(stopTime.getStop().getId());
    addToReferences(stopTime.getStop());

    return bean;
  }

  public ListWithReferencesBean<CurrentVehicleEstimateV2Bean> getCurrentVehicleEstimates(
      ListBean<CurrentVehicleEstimateBean> estimates) {

    if( estimates == null || estimates.getList() == null)
      return list(new ArrayList<CurrentVehicleEstimateV2Bean>(), false);
    
    List<CurrentVehicleEstimateV2Bean> beans = new ArrayList<CurrentVehicleEstimateV2Bean>();
    for (CurrentVehicleEstimateBean estimate : estimates.getList())
      beans.add(getCurrentVehicleEstimate(estimate));

    return list(beans, estimates.isLimitExceeded());
  }

  public CurrentVehicleEstimateV2Bean getCurrentVehicleEstimate(
      CurrentVehicleEstimateBean estimate) {

    if (estimate == null)
      return null;

    CurrentVehicleEstimateV2Bean bean = new CurrentVehicleEstimateV2Bean();
    bean.setProbability(estimate.getProbability());
    bean.setTripStatus(getTripStatus(estimate.getTripStatus()));
    bean.setDebug(estimate.getDebug());
    return bean;
  }

  public VehicleStatusV2Bean getVehicleStatus(VehicleStatusBean vehicleStatus) {

    VehicleStatusV2Bean bean = new VehicleStatusV2Bean();

    bean.setLastUpdateTime(vehicleStatus.getLastUpdateTime());
    if (vehicleStatus.getLastLocationUpdateTime() > 0)
      bean.setLastLocationUpdateTime(vehicleStatus.getLastLocationUpdateTime());
    bean.setLocation(vehicleStatus.getLocation());
    bean.setPhase(vehicleStatus.getPhase());
    bean.setStatus(vehicleStatus.getStatus());
    bean.setVehicleId(vehicleStatus.getVehicleId());

    TripBean trip = vehicleStatus.getTrip();
    if (trip != null) {
      bean.setTripId(trip.getId());
      addToReferences(trip);
    }

    TripStatusBean tripStatus = vehicleStatus.getTripStatus();
    if (tripStatus != null)
      bean.setTripStatus(getTripStatus(tripStatus));

    return bean;
  }

  public VehicleLocationRecordV2Bean getVehicleLocationRecord(
      VehicleLocationRecordBean record) {

    VehicleLocationRecordV2Bean bean = new VehicleLocationRecordV2Bean();

    bean.setBlockId(record.getBlockId());
    bean.setCurrentLocation(record.getCurrentLocation());
    if (record.isCurrentOrientationSet())
      bean.setCurrentOrientation(record.getCurrentOrientation());
    if (record.isDistanceAlongBlockSet())
      bean.setDistanceAlongBlock(record.getDistanceAlongBlock());
    bean.setPhase(record.getPhase());
    if (record.isScheduleDeviationSet())
      bean.setScheduleDeviation(record.getScheduleDeviation());
    bean.setServiceDate(record.getServiceDate());
    bean.setStatus(record.getStatus());
    bean.setTimeOfRecord(record.getTimeOfRecord());
    bean.setTimeOfLocationUpdate(record.getTimeOfLocationUpdate());
    bean.setTripId(record.getTripId());
    bean.setVehicleId(record.getVehicleId());
    return bean;
  }

  public StopScheduleV2Bean getStopSchedule(StopScheduleBean stopSchedule) {

    StopScheduleV2Bean bean = new StopScheduleV2Bean();

    StopBean stop = stopSchedule.getStop();
    if (stop != null) {
      addToReferences(stop);
      bean.setStopId(stop.getId());
    }

    bean.setDate(stopSchedule.getDate().getTime());

    List<StopRouteScheduleV2Bean> stopRouteScheduleBeans = new ArrayList<StopRouteScheduleV2Bean>();

    for (StopRouteScheduleBean stopRouteSchedule : stopSchedule.getRoutes()) {
      StopRouteScheduleV2Bean stopRouteScheduleBean = getStopRouteSchedule(stopRouteSchedule);
      stopRouteScheduleBeans.add(stopRouteScheduleBean);
    }
    bean.setStopRouteSchedules(stopRouteScheduleBeans);

    /*
     * StopCalendarDaysBean days = stopSchedule.getCalendarDays();
     * bean.setTimeZone(days.getTimeZone());
     * 
     * List<StopCalendarDayV2Bean> dayBeans = new
     * ArrayList<StopCalendarDayV2Bean>(); for (StopCalendarDayBean day :
     * days.getDays()) { StopCalendarDayV2Bean dayBean =
     * getStopCalendarDay(day); dayBeans.add(dayBean); }
     * bean.setStopCalendarDays(dayBeans);
     */

    return bean;
  }

  public StopRouteScheduleV2Bean getStopRouteSchedule(
      StopRouteScheduleBean stopRouteSchedule) {

    StopRouteScheduleV2Bean bean = new StopRouteScheduleV2Bean();

    bean.setRouteId(stopRouteSchedule.getRoute().getId());
    addToReferences(stopRouteSchedule.getRoute());

    List<StopRouteDirectionScheduleV2Bean> directions = bean.getStopRouteDirectionSchedules();
    for (StopRouteDirectionScheduleBean direction : stopRouteSchedule.getDirections())
      directions.add(getStopRouteDirectionSchedule(direction));

    return bean;
  }

  public StopRouteDirectionScheduleV2Bean getStopRouteDirectionSchedule(
      StopRouteDirectionScheduleBean direction) {

    StopRouteDirectionScheduleV2Bean bean = new StopRouteDirectionScheduleV2Bean();
    bean.setTripHeadsign(direction.getTripHeadsign());

    List<ScheduleStopTimeInstanceV2Bean> stopTimes = new ArrayList<ScheduleStopTimeInstanceV2Bean>();
    for (StopTimeInstanceBean sti : direction.getStopTimes()) {
      ScheduleStopTimeInstanceV2Bean stiBean = new ScheduleStopTimeInstanceV2Bean();
      stiBean.setArrivalTime(sti.getArrivalTime());
      stiBean.setDepartureTime(sti.getDepartureTime());
      stiBean.setServiceId(sti.getServiceId());
      stiBean.setTripId(sti.getTripId());
      stiBean.setStopHeadsign(stiBean.getStopHeadsign());
      stopTimes.add(stiBean);
    }

    if (!stopTimes.isEmpty())
      bean.setScheduleStopTimes(stopTimes);

    List<ScheduleFrequencyInstanceV2Bean> frequencies = new ArrayList<ScheduleFrequencyInstanceV2Bean>();
    for (FrequencyInstanceBean freq : direction.getFrequencies()) {
      ScheduleFrequencyInstanceV2Bean freqBean = new ScheduleFrequencyInstanceV2Bean();
      freqBean.setServiceDate(freq.getServiceDate());
      freqBean.setServiceId(freq.getServiceId());
      freqBean.setTripId(freq.getTripId());
      freqBean.setStartTime(freq.getStartTime());
      freqBean.setEndTime(freq.getEndTime());
      freqBean.setHeadway(freq.getHeadwaySecs());
      freqBean.setStopHeadsign(freq.getStopHeadsign());
      frequencies.add(freqBean);
    }

    if (!frequencies.isEmpty())
      bean.setScheduleFrequencies(frequencies);

    return bean;
  }

  public StopCalendarDayV2Bean getStopCalendarDay(StopCalendarDayBean day) {
    StopCalendarDayV2Bean bean = new StopCalendarDayV2Bean();
    bean.setDate(day.getDate().getTime());
    bean.setGroup(day.getGroup());
    return bean;
  }

  public StopsForRouteV2Bean getStopsForRoute(StopsForRouteBean stopsForRoute,
      boolean includePolylines) {
    StopsForRouteV2Bean bean = new StopsForRouteV2Bean();

    RouteBean route = stopsForRoute.getRoute();
    if (route != null) {
      addToReferences(route);
      bean.setRouteId(route.getId());
    }

    List<String> stopIds = new ArrayList<String>();
    for (StopBean stop : stopsForRoute.getStops()) {
      stopIds.add(stop.getId());
      addToReferences(stop);
    }
    bean.setStopIds(stopIds);
    bean.setStopGroupings(stopsForRoute.getStopGroupings());
    if (!includePolylines) {
      for (StopGroupingBean grouping : stopsForRoute.getStopGroupings()) {
        for (StopGroupBean group : grouping.getStopGroups())
          group.setPolylines(null);
      }
    }
    if (includePolylines)
      bean.setPolylines(stopsForRoute.getPolylines());
    return bean;
  }

  public StopWithArrivalsAndDeparturesV2Bean getStopWithArrivalAndDepartures(
      StopWithArrivalsAndDeparturesBean sad) {
    StopWithArrivalsAndDeparturesV2Bean bean = new StopWithArrivalsAndDeparturesV2Bean();

    bean.setStopId(sad.getStop().getId());
    addToReferences(sad.getStop());

    List<ArrivalAndDepartureV2Bean> ads = new ArrayList<ArrivalAndDepartureV2Bean>();
    for (ArrivalAndDepartureBean ad : sad.getArrivalsAndDepartures())
      ads.add(getArrivalAndDeparture(ad));
    bean.setArrivalsAndDepartures(ads);

    List<String> nearbyStopIds = new ArrayList<String>();
    for (StopBean nearbyStop : sad.getNearbyStops()) {
      nearbyStopIds.add(nearbyStop.getId());
      addToReferences(nearbyStop);
    }
    bean.setNearbyStopIds(nearbyStopIds);

    List<SituationBean> situations = sad.getSituations();
    if (!CollectionsLibrary.isEmpty(situations)) {
      List<String> situationIds = new ArrayList<String>();
      for (SituationBean situation : situations) {
        addToReferences(situation);
        situationIds.add(situation.getId());
      }
      bean.setSituationIds(situationIds);
    }

    return bean;
  }

  public ArrivalAndDepartureV2Bean getArrivalAndDeparture(
      ArrivalAndDepartureBean ad) {

    TripBean trip = ad.getTrip();
    RouteBean route = trip.getRoute();
    StopBean stop = ad.getStop();

    ArrivalAndDepartureV2Bean bean = new ArrivalAndDepartureV2Bean();

    bean.setTripId(trip.getId());
    addToReferences(trip);

    bean.setServiceDate(ad.getServiceDate());
    bean.setVehicleId(ad.getVehicleId());
    bean.setStopId(stop.getId());
    addToReferences(stop);
    bean.setStopSequence(ad.getStopSequence());
    bean.setBlockTripSequence(ad.getBlockTripSequence());

    bean.setRouteId(route.getId());
    addToReferences(route);

    if (trip.getRouteShortName() != null)
      bean.setRouteShortName(trip.getRouteShortName());
    else
      bean.setRouteShortName(route.getShortName());
    bean.setRouteLongName(route.getLongName());

    bean.setTripHeadsign(trip.getTripHeadsign());

    bean.setScheduledArrivalTime(ad.getScheduledArrivalTime());
    bean.setScheduledDepartureTime(ad.getScheduledDepartureTime());
    bean.setPredictedArrivalTime(ad.getPredictedArrivalTime());
    bean.setPredictedDepartureTime(ad.getPredictedDepartureTime());

    bean.setScheduledArrivalInterval(getTimeInterval(ad.getScheduledArrivalInterval()));
    bean.setScheduledDepartureInterval(getTimeInterval(ad.getScheduledDepartureInterval()));
    bean.setPredictedArrivalInterval(getTimeInterval(ad.getPredictedArrivalInterval()));
    bean.setPredictedDepartureInterval(getTimeInterval(ad.getPredictedDepartureInterval()));

    if (ad.getFrequency() != null)
      bean.setFrequency(getFrequency(ad.getFrequency()));

    bean.setStatus(ad.getStatus());

    if (ad.isDistanceFromStopSet())
      bean.setDistanceFromStop(ad.getDistanceFromStop());

    bean.setNumberOfStopsAway(ad.getNumberOfStopsAway());

    TripStatusBean tripStatus = ad.getTripStatus();
    if (tripStatus != null)
      bean.setTripStatus(getTripStatus(tripStatus));

    bean.setPredicted(ad.isPredicted());
    bean.setLastUpdateTime(ad.getLastUpdateTime());

    List<SituationBean> situations = ad.getSituations();
    if (situations != null && !situations.isEmpty()) {
      List<String> situationIds = new ArrayList<String>();
      for (SituationBean situation : situations) {
        situationIds.add(situation.getId());
        addToReferences(situation);
      }
      bean.setSituationIds(situationIds);
    }

    return bean;
  }

  public FrequencyV2Bean getFrequency(FrequencyBean frequency) {
    FrequencyV2Bean bean = new FrequencyV2Bean();
    bean.setStartTime(frequency.getStartTime());
    bean.setEndTime(frequency.getEndTime());
    bean.setHeadway(frequency.getHeadway());
    return bean;
  }

  public FrequencyBean reverseFrequency(FrequencyV2Bean frequency) {
    FrequencyBean bean = new FrequencyBean();
    bean.setStartTime(frequency.getStartTime());
    bean.setEndTime(frequency.getEndTime());
    bean.setHeadway(frequency.getHeadway());
    return bean;
  }

  public boolean isSituationExcludedForApplication(SituationBean situation) {
    SituationAffectsBean affects = situation.getAffects();
    if (affects == null)
      return false;
    List<SituationAffectedApplicationBean> applications = affects.getApplications();
    if (CollectionsLibrary.isEmpty(applications))
      return false;
    if (_applicationKey == null)
      return true;
    for (SituationAffectedApplicationBean application : applications) {
      if (_applicationKey.equals(application.getApiKey()))
        return false;
    }
    return true;
  }

  public SituationV2Bean getSituation(SituationBean situation) {

    SituationV2Bean bean = new SituationV2Bean();

    bean.setId(situation.getId());
    bean.setCreationTime(situation.getCreationTime());

    if (situation.getPublicationWindow() != null)
      bean.setPublicationWindow(getTimeRange(situation.getPublicationWindow()));

    if (situation.getAffects() != null)
      bean.setAffects(getSituationAffects(situation.getAffects()));

    if (!CollectionsLibrary.isEmpty(situation.getConsequences())) {
      List<SituationConsequenceV2Bean> beans = new ArrayList<SituationConsequenceV2Bean>();
      for (SituationConsequenceBean consequence : situation.getConsequences()) {
        SituationConsequenceV2Bean consequenceBean = getSituationConsequence(consequence);
        beans.add(consequenceBean);
      }
      bean.setConsequences(beans);
    }

    bean.setEnvironmentReason(situation.getEnvironmentReason());
    bean.setEquipmentReason(situation.getEquipmentReason());
    bean.setPersonnelReason(situation.getPersonnelReason());
    bean.setMiscellaneousReason(situation.getMiscellaneousReason());
    bean.setUndefinedReason(situation.getUndefinedReason());

    bean.setSummary(getString(situation.getSummary()));
    bean.setDescription(getString(situation.getDescription()));
    bean.setAdvice(getString(situation.getAdvice()));

    bean.setDetail(getString(situation.getDetail()));
    bean.setInternal(getString(situation.getInternal()));

    ESeverity severity = situation.getSeverity();
    if (severity != null) {
      String[] codes = severity.getTpegCodes();
      bean.setSeverity(codes[0]);
    }

    ESensitivity sensitivity = situation.getSensitivity();
    if (sensitivity != null)
      bean.setSensitivity(sensitivity.getXmlValue());

    return bean;
  }

  public SituationAffectsV2Bean getSituationAffects(SituationAffectsBean affects) {

    SituationAffectsV2Bean bean = new SituationAffectsV2Bean();

    List<SituationAffectedAgencyBean> agencies = affects.getAgencies();

    if (!CollectionsLibrary.isEmpty(agencies)) {
      List<SituationAffectedAgencyV2Bean> beans = new ArrayList<SituationAffectedAgencyV2Bean>();
      for (SituationAffectedAgencyBean agency : agencies)
        beans.add(getSituationAffectedAgency(agency));
      bean.setAgencies(beans);
    }

    List<SituationAffectedStopBean> stops = affects.getStops();

    if (!CollectionsLibrary.isEmpty(stops)) {
      List<SituationAffectedStopV2Bean> beans = new ArrayList<SituationAffectedStopV2Bean>();
      for (SituationAffectedStopBean stop : stops)
        beans.add(getSituationAffectedStop(stop));
      bean.setStops(beans);
    }

    List<SituationAffectedVehicleJourneyBean> journeys = affects.getVehicleJourneys();

    if (!CollectionsLibrary.isEmpty(journeys)) {
      List<SituationAffectedVehicleJourneyV2Bean> beans = new ArrayList<SituationAffectedVehicleJourneyV2Bean>();
      for (SituationAffectedVehicleJourneyBean journey : journeys)
        beans.add(getSituationAffectedJourney(journey));
      bean.setVehicleJourneys(beans);
    }

    List<SituationAffectedApplicationBean> applications = affects.getApplications();

    if (!CollectionsLibrary.isEmpty(applications)) {
      List<SituationAffectedApplicationV2Bean> beans = new ArrayList<SituationAffectedApplicationV2Bean>();
      for (SituationAffectedApplicationBean application : applications) {
        beans.add(getSituationAffectedApplication(application));
      }
      bean.setApplications(beans);
    }

    return bean;
  }

  public SituationAffectedVehicleJourneyV2Bean getSituationAffectedJourney(
      SituationAffectedVehicleJourneyBean journey) {
    SituationAffectedVehicleJourneyV2Bean bean = new SituationAffectedVehicleJourneyV2Bean();
    bean.setLineId(journey.getLineId());
    bean.setDirectionId(journey.getDirection());

    if (!CollectionsLibrary.isEmpty(journey.getCalls())) {
      List<SituationAffectedCallV2Bean> calls = new ArrayList<SituationAffectedCallV2Bean>();
      for (SituationAffectedCallBean call : journey.getCalls()) {
        SituationAffectedCallV2Bean callBean = new SituationAffectedCallV2Bean();
        callBean.setStopId(call.getStopId());
        calls.add(callBean);
      }
      bean.setCalls(calls);
    }
    return bean;
  }

  public SituationAffectedAgencyV2Bean getSituationAffectedAgency(
      SituationAffectedAgencyBean agency) {
    SituationAffectedAgencyV2Bean bean = new SituationAffectedAgencyV2Bean();
    bean.setAgencyId(agency.getAgencyId());
    return bean;
  }

  public SituationAffectedStopV2Bean getSituationAffectedStop(
      SituationAffectedStopBean stop) {
    SituationAffectedStopV2Bean bean = new SituationAffectedStopV2Bean();
    bean.setStopId(stop.getStopId());
    return bean;
  }

  public SituationAffectedApplicationV2Bean getSituationAffectedApplication(
      SituationAffectedApplicationBean app) {
    SituationAffectedApplicationV2Bean bean = new SituationAffectedApplicationV2Bean();
    bean.setApiKey(app.getApiKey());
    return bean;
  }

  private SituationConsequenceV2Bean getSituationConsequence(
      SituationConsequenceBean consequence) {

    SituationConsequenceV2Bean bean = new SituationConsequenceV2Bean();

    if (consequence.getPeriod() != null)
      bean.setPeriod(getTimeRange(consequence.getPeriod()));

    bean.setCondition(consequence.getCondition());

    SituationConditionDetailsBean details = consequence.getConditionDetails();
    if (_includeConditionDetails && details != null) {
      SituationConditionDetailsV2Bean detailsBean = new SituationConditionDetailsV2Bean();
      detailsBean.setDiversionPath(details.getDiversionPath());
      detailsBean.setDiversionStopIds(details.getDiversionStopIds());
      bean.setConditionDetails(detailsBean);
    }
    return bean;
  }

  public AgencyWithCoverageV2Bean getAgencyWithCoverage(
      AgencyWithCoverageBean awc) {

    AgencyWithCoverageV2Bean bean = new AgencyWithCoverageV2Bean();

    bean.setAgencyId(awc.getAgency().getId());
    bean.setLat(awc.getLat());
    bean.setLon(awc.getLon());
    bean.setLatSpan(awc.getLatSpan());
    bean.setLonSpan(awc.getLonSpan());

    addToReferences(awc.getAgency());

    return bean;
  }

  public NaturalLanguageStringV2Bean getString(NaturalLanguageStringBean nls) {
    if (nls == null)
      return null;
    if (nls.getValue() == null || nls.getValue().isEmpty())
      return null;
    NaturalLanguageStringV2Bean bean = new NaturalLanguageStringV2Bean();
    bean.setLang(nls.getLang());
    bean.setValue(nls.getValue());
    return bean;
  }

  public TimeRangeV2Bean getTimeRange(TimeRangeBean range) {
    if (range == null)
      return null;
    TimeRangeV2Bean bean = new TimeRangeV2Bean();
    bean.setFrom(range.getFrom());
    bean.setTo(range.getTo());
    return bean;
  }

  public CoordinatePointV2Bean getPoint(CoordinatePoint point) {
    if (point == null)
      return null;
    CoordinatePointV2Bean bean = new CoordinatePointV2Bean();
    bean.setLat(point.getLat());
    bean.setLon(point.getLon());
    return bean;
  }

  public CoordinatePoint reversePoint(CoordinatePointV2Bean bean) {
    if (bean == null)
      return null;
    return new CoordinatePoint(bean.getLat(), bean.getLon());
  }

  /****
   * References Methods
   ****/

  public void addToReferences(AgencyBean agency) {
    if (!shouldAddReferenceWithId(_references.getAgencies(), agency.getId()))
      return;
    AgencyV2Bean bean = getAgency(agency);
    _references.addAgency(bean);
  }

  public void addToReferences(RouteBean route) {
    if (!shouldAddReferenceWithId(_references.getRoutes(), route.getId()))
      return;
    RouteV2Bean bean = getRoute(route);
    _references.addRoute(bean);
  }

  public void addToReferences(StopBean stop) {
    if (!shouldAddReferenceWithId(_references.getStops(), stop.getId()))
      return;
    StopV2Bean bean = getStop(stop);
    _references.addStop(bean);
  }

  public void addToReferences(TripBean trip) {
    if (!shouldAddReferenceWithId(_references.getTrips(), trip.getId()))
      return;
    TripV2Bean bean = getTrip(trip);
    _references.addTrip(bean);
  }

  public void addToReferences(SituationBean situation) {
    if (isSituationExcludedForApplication(situation))
      return;
    if (!shouldAddReferenceWithId(_references.getSituations(),
        situation.getId()))
      return;
    SituationV2Bean bean = getSituation(situation);
    _references.addSituation(bean);
  }

  /****
   * Private Methods
   ****/

  public <T> EntryWithReferencesBean<T> entry(T entry) {
    return new EntryWithReferencesBean<T>(entry, _references);
  }

  public <T> ListWithReferencesBean<T> list(List<T> list, boolean limitExceeded) {
    return new ListWithReferencesBean<T>(list, limitExceeded, _references);
  }

  public <T> ListWithReferencesBean<T> list(List<T> list,
      boolean limitExceeded, boolean outOfRange) {
    return new ListWithRangeAndReferencesBean<T>(list, limitExceeded,
        outOfRange, _references);
  }

  public boolean isStringSet(String value) {
    return value != null && !value.isEmpty();
  }

  private <T> List<T> filter(List<T> beans) {
    if (_maxCount == null)
      return beans;
    return _maxCount.filter(beans, false);
  }

  private <T extends HasId> boolean shouldAddReferenceWithId(
      Iterable<T> entities, String id) {

    if (!_includeReferences)
      return false;

    if (entities == null)
      return true;

    for (T entity : entities) {
      if (entity.getId().equals(id))
        return false;
    }

    return true;
  }
}
