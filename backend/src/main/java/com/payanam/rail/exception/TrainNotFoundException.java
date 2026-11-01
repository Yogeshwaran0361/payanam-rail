package com.payanam.rail.exception;

/** Thrown when a train number is not in the timetable (test F3). */
public class TrainNotFoundException extends RailwayException {
    public TrainNotFoundException(String message) {
        super("TRAIN_NOT_FOUND", message);
    }
}
