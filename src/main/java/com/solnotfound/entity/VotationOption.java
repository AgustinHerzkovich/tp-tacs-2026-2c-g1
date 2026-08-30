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

  public synchronized List<User> getUsers() {
    return List.copyOf(users);
  }

  public synchronized void setUsers(List<User> users) {
    this.users = new ArrayList<>(users);
  }

  public synchronized void addUser(User user) {
    this.users.add(user);
  }

  public synchronized boolean thisUserVoted(User user) {
    return this.users.contains(user);
  }

  public synchronized void removeUser(User user) {
    this.users.remove(user);
  }
}
