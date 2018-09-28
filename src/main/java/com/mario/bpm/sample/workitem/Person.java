package com.mario.bpm.sample.workitem;

import lombok.*;

import javax.persistence.*;

import static javax.persistence.GenerationType.SEQUENCE;

@Entity
@Table(
  name = "person",
  indexes = {@Index(name = "person_name_idx", columnList = "name", unique = true)}
)
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"id"})
@ToString
@Getter
@Setter
public class Person {
  @Id
  @GeneratedValue(strategy = SEQUENCE, generator = "person_generator")
  @SequenceGenerator(
    name = "person_generator",
    sequenceName = "person_seq",
    allocationSize = 1
  )

  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  public Person(String name) {
    this.name = name;
  }
}
