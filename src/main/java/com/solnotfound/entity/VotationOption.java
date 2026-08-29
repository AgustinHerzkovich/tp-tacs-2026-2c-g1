package com.solnotfound.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class VotationOption {

  private LocalDateTime dateTime;
  private List<User> users;

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public List<User> getUsers() {
    return users;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public void setUsers(List<User> users) {
    this.users = users;
  }
}
