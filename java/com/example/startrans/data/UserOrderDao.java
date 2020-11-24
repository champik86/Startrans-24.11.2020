package com.example.startrans.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserOrderDao {
    @Query("SELECT * FROM userOrder")
    List<UserOrder> getAll();

    @Query("SELECT * FROM userOrder WHERE id = :id")
    UserOrder getById (int id);

    @Insert
    void insert (UserOrder userOrder);

    @Update
    void update (UserOrder userOrder);

    @Delete
    void delete (UserOrder userOrder);
}
