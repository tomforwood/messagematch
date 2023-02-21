package org.forwoods.messagematch.jpa.project;


import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "MY_ENTITY")
public class MyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "myVal")
    private String value;

    @Column(name = "myValInt")
    private int valueInt;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getValueInt() {
        return valueInt;
    }

    public void setValueInt(int valueInt) {
        this.valueInt = valueInt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MyEntity myEntity = (MyEntity) o;
        return valueInt == myEntity.valueInt && id.equals(myEntity.id) && Objects.equals(value, myEntity.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, value, valueInt);
    }
}
