package com.parkit.parkingsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

	private static ParkingService parkingService;

	@Mock
	private static InputReaderUtil inputReaderUtil;
	@Mock
	private static ParkingSpotDAO parkingSpotDAO;
	@Mock
	private static TicketDAO ticketDAO;


	@BeforeEach
	private void setUpPerTest() {		
		parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
	}

	
	private void setUpExitingTestCar() {
		try {
			when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
		} catch (Exception e) {
			
			e.printStackTrace();
			throw new RuntimeException("Failed to set up test mock objects");
		}
		final ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
		final Ticket ticket = new Ticket();
		ticket.setInTime(new Date(System.currentTimeMillis() - 60 * 60 * 1000));
		ticket.setParkingSpot(parkingSpot);
		ticket.setVehicleRegNumber("ABCDEF");
		when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
		when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
	}
	
	private void setUpExitingTestBike() {
		try {
			when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
		} catch (Exception e) {
			
			e.printStackTrace();
			throw new RuntimeException("Failed to set up test mock objects");
		}
		final ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);
		final Ticket ticket = new Ticket();
		ticket.setInTime(new Date(System.currentTimeMillis() - 60 * 60 * 1000));
		ticket.setParkingSpot(parkingSpot);
		ticket.setVehicleRegNumber("ABCDEF");
		when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
		when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
	}
		
	@Test
    public void processExitingVehicleTest(){
		setUpExitingTestCar();
		when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
		when(ticketDAO.getNbTicket(any(Ticket.class))).thenReturn((long) 1);
		parkingService.processExitingVehicle();
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));

    }
	
	@Test
    public void processExitingVehicleBikeTest(){
		setUpExitingTestBike();
		when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
		when(ticketDAO.getNbTicket(any(Ticket.class))).thenReturn((long) 1);
		parkingService.processExitingVehicle();
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }
	
	@Test
    public void processExitingVehicleTestCarWithDiscount(){
		setUpExitingTestCar();
		when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
		when(ticketDAO.getNbTicket(any(Ticket.class))).thenReturn((long) 2);
		parkingService.processExitingVehicle();
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));

    }
	
	// test de l’appel de la méthode processIncomingVehicle() où tout se déroule
	// comme attendu.
	@Test
    public void testProcessIncomingVehicle(){
		try {
			when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to set up test mock objects");
		}
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(2);
	    parkingService.processIncomingVehicle();
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
	}

	// exécution du test dans le cas où la méthode updateTicket() de ticketDAO
	// renvoie false lors de l’appel de processExitingVehicle()
	@Test
	public void processExitingVehicleTestUnableUpdate(){
		setUpExitingTestCar();
		when(ticketDAO.getNbTicket(any(Ticket.class))).thenReturn((long) 1);
        parkingService.processExitingVehicle();
	}

	// test de l’appel de la méthode getNextParkingNumberIfAvailable() avec pour
	// résultat l’obtention d’un spot dont l’ID est 1 et qui est disponible.
	@Test
    public void testGetNextParkingNumberIfAvailable(){
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        final ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);
        assertEquals(parkingService.getNextParkingNumberIfAvailable(), parkingSpot);
    }

	// test de l’appel de la méthode getNextParkingNumberIfAvailable() avec pour
	// résultat aucun spot disponible (la méthode renvoie null).
	@Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound(){
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(-1);
        assertEquals(parkingService.getNextParkingNumberIfAvailable(), null);
    }

	// test de l’appel de la méthode getNextParkingNumberIfAvailable() avec pour
	// résultat aucun spot (la méthode renvoie null) car l’argument saisi par
	// l’utilisateur concernant le type de véhicule est erroné (par exemple,
	// l’utilisateur a saisi 3).
	@Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument(){
		when(inputReaderUtil.readSelection()).thenReturn(3);
		assertEquals(parkingService.getNextParkingNumberIfAvailable(), null);
    }

}
