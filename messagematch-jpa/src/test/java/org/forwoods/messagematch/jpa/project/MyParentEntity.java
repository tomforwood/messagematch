package org.forwoods.messagematch.jpa.project;

import jakarta.persistence.*;

@Entity
@Table(name = "MY_PARENT_ENTITY")
public class MyParentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;


    @ManyToOne
    @JoinColumn(name = "my_entity_id")
    private MyEntity myEntity;

    public MyEntity getMyEntity() {
        return myEntity;
    }

    public void setMyEntity(MyEntity myEntity) {
        this.myEntity = myEntity;
    }
}
