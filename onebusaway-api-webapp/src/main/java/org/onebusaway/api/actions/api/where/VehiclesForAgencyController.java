package org.onebusaway.api.actions.api.where;

import java.io.IOException;

import org.apache.struts2.rest.DefaultHttpHeaders;
import org.onebusaway.api.actions.api.ApiActionSupport;
import org.onebusaway.api.model.transit.BeanFactoryV2;
import org.onebusaway.api.model.transit.VehicleStatusV2Bean;
import org.onebusaway.exceptions.OutOfServiceAreaServiceException;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.validator.annotations.RequiredFieldValidator;

public class VehiclesForAgencyController extends ApiActionSupport {

  private static final long serialVersionUID = 1L;

  private static final int V2 = 2;

  @Autowired
  private TransitDataService _service;

  private String _id;

  private long _time = 0;

  public VehiclesForAgencyController() {
    super(V2);
  }

  @RequiredFieldValidator
  public void setId(String id) {
    _id = id;
  }

  public String getId() {
    return _id;
  }

  public void setTime(long time) {
    _time = time;
  }

  public DefaultHttpHeaders show() throws IOException, ServiceException {

    if (!isVersion(V2))
      return setUnknownVersionResponse();

    if (hasErrors())
      return setValidationErrorsResponse();

    long time = System.currentTimeMillis();
    if (_time != 0)
      time = _time;

    BeanFactoryV2 factory = getBeanFactoryV2();

    try {
      ListBean<VehicleStatusBean> vehicles = _service.getAllVehiclesForAgency(
          _id, time);
      return setOkResponse(factory.getVehicleStatusResponse(vehicles));
    } catch (OutOfServiceAreaServiceException ex) {
      return setOkResponse(factory.getEmptyList(VehicleStatusV2Bean.class, true));
    }
  }
}