package server.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.*;

@Getter
@Setter
@Entity
@Table(name = "user")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "username", nullable = false, unique = true)
  private String username;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "birth_date")
  @Temporal(TemporalType.DATE)
  private LocalDate birthDate;

  @Column(name = "phone")
  private String phone;

  @Column(name = "email")
  private String email;

  @Column(name = "address")
  private String address;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Loan> loans = new ArrayList<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return userId == user.userId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId);
  }

  @Override
  public String toString() {
    return "User{" +
            "userId=" + userId +
            ", username='" + username + '\'' +
            ", passwordHash='" + passwordHash + '\'' +
            ", role=" + role +
            '}';
  }
}