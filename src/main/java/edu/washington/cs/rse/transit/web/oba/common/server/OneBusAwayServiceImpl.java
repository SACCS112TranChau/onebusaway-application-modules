/*
 * Copyright 2008 Brian Ferris
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.washington.cs.rse.transit.web.oba.common.server;

import edu.washington.cs.rse.transit.common.model.Route;
import edu.washington.cs.rse.transit.common.model.StopLocation;
import edu.washington.cs.rse.transit.common.model.aggregate.SelectionName;
import edu.washington.cs.rse.transit.common.model.aggregate.ServicePatternBlockKey;
import edu.washington.cs.rse.transit.common.model.aggregate.StopSelectionTree;
import edu.washington.cs.rse.transit.common.services.CacheOp;
import edu.washington.cs.rse.transit.common.spring.PostConstruct;
import edu.washington.cs.rse.transit.common.spring.PreDestroy;
import edu.washington.cs.rse.transit.web.actions.GeocoderAccuracyToBounds;
import edu.washington.cs.rse.transit.web.oba.common.client.model.NameBean;
import edu.washington.cs.rse.transit.web.oba.common.client.model.NameTreeBean;
import edu.washington.cs.rse.transit.web.oba.common.client.model.PredictedArrivalBean;
import edu.washington.cs.rse.transit.web.oba.common.client.model.ServicePatternBlockPathsBean;
import edu.washington.cs.rse.transit.web.oba.common.client.model.ServicePatternBlocksBean;
import edu.washington.cs.rse.transit.web.oba.common.client.model.StopAreaBean;
import edu.washington.cs.rse.transit.web.oba.common.client.model.StopBean;
import edu.washington.cs.rse.transit.web.oba.common.client.model.StopWithArrivalsBean;
import edu.washington.cs.rse.transit.web.oba.common.client.model.StopWithRoutesBean;
import edu.washington.cs.rse.transit.web.oba.common.client.model.StopsBean;
import edu.washington.cs.rse.transit.web.oba.common.client.rpc.InvalidSelectionServiceException;
import edu.washington.cs.rse.transit.web.oba.common.client.rpc.NoSuchRouteServiceException;
import edu.washington.cs.rse.transit.web.oba.common.client.rpc.OneBusAwayService;
import edu.washington.cs.rse.transit.web.oba.common.client.rpc.ServiceException;
import edu.washington.cs.rse.transit.web.oba.common.server.ops.PredictedArrivalsOp;
import edu.washington.cs.rse.transit.web.oba.common.server.ops.ServicePatternBlocksOp;
import edu.washington.cs.rse.transit.web.oba.common.server.ops.ServicePatternPathOp;
import edu.washington.cs.rse.transit.web.oba.common.server.ops.StopAreaOp;
import edu.washington.cs.rse.transit.web.oba.common.server.ops.StopSelectionTreeOp;
import edu.washington.cs.rse.transit.web.oba.common.server.ops.StopWithRoutesOp;
import edu.washington.cs.rse.transit.web.oba.common.server.ops.StopsByServicePatternOp;

import com.vividsolutions.jts.geom.Geometry;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class OneBusAwayServiceImpl extends ApplicationBeanSupport implements
    OneBusAwayService {

  private static final long serialVersionUID = 1L;

  private CacheManager _cacheManager;

  private boolean _useCache = false;

  /***************************************************************************
   * Operations
   **************************************************************************/

  private Set<CacheOp<?, ?>> _ops = new LinkedHashSet<CacheOp<?, ?>>();

  @Autowired
  private StopsByServicePatternOp _stopsByServicePatternOp;

  @Autowired
  private ServicePatternPathOp _servicePatternPathOp;

  @Autowired
  private StopWithRoutesOp _stopsWithRoutesOp;

  @Autowired
  private StopSelectionTreeOp _stopSelectionTreeOp;

  @Autowired
  private ServicePatternBlocksOp _servicePatternTimeBlocksOp;

  @Autowired
  private StopAreaOp _stopAreaOp;

  @Autowired
  private PredictedArrivalsOp _arrivalsOp;

  /***************************************************************************
   * Public Methods
   **************************************************************************/

  @Autowired
  public void setCacheManager(CacheManager cacheManager) {
    _cacheManager = cacheManager;
  }

  @PostConstruct
  public void startup() {

    // Preload to cache
    _dao.getAllRoutes();
    _dao.getAllServicePatterns();

    addOp(_stopsWithRoutesOp);
    addOp(_servicePatternTimeBlocksOp);
    addOp(_servicePatternPathOp);
    addOp(_stopsByServicePatternOp);
    addOp(_stopSelectionTreeOp);
    addOp(_stopAreaOp);
    addOp(_arrivalsOp);

    startupOps();
  }

  @PreDestroy
  public void shutdown() {

    shutdownOps();
    for (CacheOp<?, ?> op : _ops) {
      Cache cache = getCacheByRegion(op.getClass());
      cache.flush();
    }
  }

  private void startupOps() {
    for (CacheOp<?, ?> op : _ops)
      op.startup(getCacheByRegion(op.getClass()));
  }

  private void shutdownOps() {
    for (CacheOp<?, ?> op : _ops)
      op.shutdown();
  }

  /***************************************************************************
   * {@link OneBusAwayService} Interface
   **************************************************************************/

  public StopsBean getStopsByLocationAndAccuracy(double lat, double lon,
      int accuracy) {
    int r = GeocoderAccuracyToBounds.getBoundsInFeetByAccuracy(accuracy);
    Geometry g = _dao.getLatLonAsPoint(lat, lon);
    g = g.buffer(r).getEnvelope();
    return getStopsByGeometry(g);
  }

  public StopsBean getStopsByBounds(double lat1, double lon1, double lat2,
      double lon2) throws ServiceException {
    Geometry p1 = _dao.getLatLonAsPoint(lat1, lon1);
    Geometry p2 = _dao.getLatLonAsPoint(lat2, lon2);
    Geometry g = p1.union(p2);
    g = g.getEnvelope();
    return getStopsByGeometry(g);
  }

  public StopWithRoutesBean getStop(String stopIdString)
      throws ServiceException {
    int stopId = Integer.parseInt(stopIdString);
    return cache(_stopsWithRoutesOp, stopId);
  }

  public StopWithArrivalsBean getArrivalsByStopId(String stopIdString)
      throws ServiceException {

    try {
      int stopId = Integer.parseInt(stopIdString);
      StopAreaBean bean = cache(_stopAreaOp, stopId);
      List<PredictedArrivalBean> arrivals = cache(_arrivalsOp, stopId);
      StopWithArrivalsBean arrivalsBean = new StopWithArrivalsBean(bean,
          arrivals);

      return arrivalsBean;

    } catch (Exception ex) {

      if (ex instanceof ServiceException)
        throw (ServiceException) ex;

      ex.printStackTrace();
      throw new ServiceException("bummer=" + ex.getMessage());
    }
  }

  public NameTreeBean getStopByRoute(String route, List<Integer> selection)
      throws ServiceException {
    NameTreeBean bean = new NameTreeBean();
    int routeId = Integer.parseInt(route);
    StopSelectionTree tree = cache(_stopSelectionTreeOp, routeId);
    visitTree(tree, bean, selection, 0);
    return bean;
  }

  public ServicePatternBlocksBean getServicePatternBlocksByRoute(String route)
      throws ServiceException {
    int routeId = Integer.parseInt(route);
    return cache(_servicePatternTimeBlocksOp, routeId);
  }

  public ServicePatternBlockPathsBean getServicePatternPath(String routeNumber,
      String servicePatternId) throws ServiceException {
    ServicePatternBlockKey key = getServicePatternBlockKey(routeNumber,
        servicePatternId);
    return cache(_servicePatternPathOp, key);
  }

  public StopsBean getActiveStopsByServicePattern(String routeNumber,
      String servicePatternId) throws ServiceException {
    ServicePatternBlockKey key = getServicePatternBlockKey(routeNumber,
        servicePatternId);
    return cache(_stopsByServicePatternOp, key);
  }

  /***************************************************************************
   * 
   **************************************************************************/

  private ServicePatternBlockKey getServicePatternBlockKey(String routeNumber,
      String servicePatternId) throws NoSuchRouteServiceException {
    Route route = _dao.getRouteByNumber(Integer.parseInt(routeNumber));
    if (route == null)
      throw new NoSuchRouteServiceException();
    return new ServicePatternBlockKey(route, servicePatternId);
  }

  private StopsBean getStopsByGeometry(Geometry g) {

    List<StopLocation> stops = _dao.getStopLocationsByLocation(g);

    List<StopBean> stopBeans = new ArrayList<StopBean>();

    for (StopLocation stop : stops) {
      StopBean sb = getStopAsBean(stop);
      stopBeans.add(sb);
    }

    StopsBean stopsBean = new StopsBean();
    stopsBean.setStopBeans(stopBeans);
    return stopsBean;
  }

  /***************************************************************************
   * Tree Methods
   **************************************************************************/

  private void visitTree(StopSelectionTree tree, NameTreeBean bean,
      List<Integer> selection, int index)
      throws InvalidSelectionServiceException {

    // If we have a stop, we have no choice but to return
    if (tree.hasStop()) {
      bean.setStop(getStopAsBean(tree.getStop()));
      return;
    }

    Set<SelectionName> names = tree.getNames();

    // If we've only got one name, short circuit
    if (names.size() == 1) {

      SelectionName next = names.iterator().next();
      bean.addSelected(getNameAsBean(next));

      StopSelectionTree subtree = tree.getSubTree(next);
      visitTree(subtree, bean, selection, index);

      return;
    }

    if (index >= selection.size()) {

      for (SelectionName name : names) {
        NameBean n = getNameAsBean(name);
        StopLocation stop = getStop(tree.getSubTree(name));
        if (stop != null) {
          bean.addNameWithStop(n, getStopAsBean(stop));
        } else {
          bean.addName(n);
        }
      }

      List<StopLocation> stops = tree.getAllStops();

      for (StopLocation stop : stops)
        bean.addStop(getStopAsBean(stop));

      return;

    } else {

      int i = 0;
      int selectionIndex = selection.get(index);

      for (SelectionName name : names) {
        if (selectionIndex == i) {
          bean.addSelected(getNameAsBean(name));
          tree = tree.getSubTree(name);
          visitTree(tree, bean, selection, index + 1);
          return;
        }
        i++;
      }
    }

    // If we made it here...
    throw new InvalidSelectionServiceException();
  }

  private StopLocation getStop(StopSelectionTree tree) {

    if (tree.hasStop())
      return tree.getStop();

    if (tree.getNames().size() == 1) {
      SelectionName next = tree.getNames().iterator().next();
      return getStop(tree.getSubTree(next));
    }

    return null;
  }

  /***************************************************************************
   * Cache Interaction Methods
   **************************************************************************/

  private <T extends CacheOp<?, ?>> T addOp(T op) {
    _ops.add(op);
    return op;
  }

  @SuppressWarnings("unchecked")
  private <KEY extends Serializable, VALUE> VALUE cache(CacheOp<KEY, VALUE> op,
      KEY key) throws ServiceException {

    if (!_useCache)
      return op.evaluate(key);

    Cache cache = getCacheByRegion(op.getClass());
    Element element = cache.get(key);
    if (element == null) {
      VALUE value = op.evaluate(key);
      element = new Element(key, value);
      cache.put(element);
    }
    return (VALUE) element.getValue();
  }

  private Cache getCacheByRegion(Class<?> opClass) {
    Cache cache = _cacheManager.getCache(opClass.getName());
    if (cache == null) {
      System.out.println("CREATING CACHE=" + opClass.getName());
      _cacheManager.addCache(opClass.getName());
      cache = _cacheManager.getCache(opClass.getName());
    }
    return cache;
  }

}
