package org.opennms.taskset.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Deprecated
public class Result {

  private String uuid;
  private Map<String, Object> parameters = new LinkedHashMap<>();
  private String status;
  private String reason;

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Result)) {
      return false;
    }
    Result result = (Result) o;
    return Objects.equals(getUuid(), result.getUuid()) && Objects.equals(
        getParameters(), result.getParameters()) && Objects.equals(getStatus(),
        result.getStatus()) && Objects.equals(getReason(), result.getReason());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getUuid(), getParameters(), getStatus(), getReason());
  }

  public String toString() {
    return "Result [" + uuid + " " + status + " " + reason + " " + parameters + "]";
  }

}
