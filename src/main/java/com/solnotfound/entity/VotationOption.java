package com.solnotfound.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VotationOption {

  private LocalDateTime dateTime;
  private List<User> users = new ArrayList<>();

  public List<User> getUsers() {
    return List.copyOf(users);
  }

  public void setUsers(List<User> users) {
    this.users = new ArrayList<>(users);
  }

  public void addUser(User user) {
    this.users.add(user);
  }

  public boolean thisUserVoted(User user) {
    return this.users.contains(user);
  }

  public void removeUser(User user) {
    this.users.remove(user);
  }
}
