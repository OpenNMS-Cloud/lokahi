package org.opennms.taskset.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Deprecated
public class Results {

  private final List<Result> results = new ArrayList<>();

  public List<Result> getResults() {
    return results;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Results)) {
      return false;
    }
    Results results = (Results) o;
    return Objects.equals(getResults(), results.getResults());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getResults());
  }

  public String toString() {
    return "Results " + results;
  }

}
