package com.chess.chessapi.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Entity
@Table(name = "category")
@JsonIdentityInfo(generator= ObjectIdGenerators.PropertyGenerator.class, property="id",scope = Category.class)
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NotNull
    @Length(max = 1000,message = "name is required not large than 1000 characters")
    private String name;

    @OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY,mappedBy = "category")
    private List<CategoryHasCourse> categoryHasCourses;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CategoryHasCourse> getCategoryHasCourses() {
        return categoryHasCourses;
    }

    public void setCategoryHasCourses(List<CategoryHasCourse> categoryHasCourses) {
        this.categoryHasCourses = categoryHasCourses;
    }
}