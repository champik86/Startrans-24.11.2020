package com.example.startrans.data;

import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {UserOrder.class}, version = 1)
public abstract class UserOrderDatabase extends RoomDatabase {
    public abstract UserOrderDao userOrderDao();
}
