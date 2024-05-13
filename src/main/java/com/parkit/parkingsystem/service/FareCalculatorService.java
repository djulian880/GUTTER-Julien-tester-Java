package com.parkit.parkingsystem.service;

import java.time.Duration;
import java.util.Date;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }
        Duration d=Duration.between(ticket.getInTime().toInstant(),ticket.getOutTime().toInstant());
        
        if(isFareFree(d)) {
        	ticket.setPrice(0.0);
        }
        else {
        	double duration=new Long(d.toMinutes()).doubleValue()/Fare.NB_MINUTES_PER_HOUR;
        	switch (ticket.getParkingSpot().getParkingType()){
	            case CAR: {
	                ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
	                break;
	            }
	            case BIKE: {
	                ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
	                break;
	            }
	            default: throw new IllegalArgumentException("Unkown Parking Type");
	        }
        }
    }
    
    public boolean isFareFree(Duration d) {
    	if(d.toMinutes()<Fare.FREE_DURATION_IN_MINUTES) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }
}