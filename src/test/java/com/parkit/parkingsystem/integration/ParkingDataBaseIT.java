package com.parkit.parkingsystem.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

	private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
	private static ParkingSpotDAO parkingSpotDAO;
	private static TicketDAO ticketDAO;
	private static DataBasePrepareService dataBasePrepareService;

	@Mock
	private static InputReaderUtil inputReaderUtil;

	@BeforeAll
	private static void setUp() throws Exception {
		parkingSpotDAO = new ParkingSpotDAO();
		parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
		ticketDAO = new TicketDAO();
		ticketDAO.dataBaseConfig = dataBaseTestConfig;
		dataBasePrepareService = new DataBasePrepareService();

	}

	@BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

	@AfterAll
	private static void tearDown() {

	}

	@Test
	// check that a ticket is actualy saved in DB and Parking table is updated with
	// availability
	public void testParkingACar() {
		final ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processIncomingVehicle();

		Connection con = null;
		try {
			con = dataBaseTestConfig.getConnection();
			final PreparedStatement ps = con
					.prepareStatement("select count(*) from ticket where VEHICLE_REG_NUMBER='ABCDEF'");
			ps.execute();
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				assertThat(rs.getInt(1)).isEqualTo(1);
			}
			dataBaseTestConfig.closeResultSet(rs);
			dataBaseTestConfig.closePreparedStatement(ps);

			final PreparedStatement ps2 = con.prepareStatement(
					"select parking.available from ticket, parking where ticket.vehicle_reg_number='ABCDEF' "
							+ "and ticket.parking_number=parking.parking_number");
			ps2.execute();
			final ResultSet rs2 = ps2.executeQuery();
			if (rs2.next()) {
				assertThat(rs2.getBoolean(1)).isEqualTo(false);

			}
			dataBaseTestConfig.closeResultSet(rs2);
			dataBaseTestConfig.closePreparedStatement(ps2);
		} catch (final Exception ex) {
			System.out.println("Error setting testParkingACar");
			ex.printStackTrace();
		} finally {
			dataBaseTestConfig.closeConnection(con);
		}
	}

	@Test
	// check that the fare generated and out time are populated correctly in the
	// database
	public void testParkingLotExit() {

		final ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processIncomingVehicle();

		final Date newInTime = new Date();
		newInTime.setTime(System.currentTimeMillis() - 60 * 60 * 1000);

		Connection con = null;
		try {
			con = dataBaseTestConfig.getConnection();

			final PreparedStatement ps = con
					.prepareStatement("update ticket set IN_TIME=? where VEHICLE_REG_NUMBER='ABCDEF'");
			ps.setTimestamp(1, new Timestamp(newInTime.getTime()));
			ps.execute();

			dataBaseTestConfig.closePreparedStatement(ps);

			parkingService.processExitingVehicle();

			final PreparedStatement ps2 = con
					.prepareStatement("select price, out_time from ticket where ticket.vehicle_reg_number='ABCDEF'");
			ps2.execute();
			final ResultSet rs2 = ps2.executeQuery();
			if (rs2.next()) {
				assertThat(rs2.getDouble(1)).isEqualTo(Fare.CAR_RATE_PER_HOUR);
				assertThat(rs2.getTimestamp(2)).isCloseTo(new Timestamp(new Date().getTime()), 10000);

			}
			dataBaseTestConfig.closePreparedStatement(ps2);
			dataBaseTestConfig.closeResultSet(rs2);

		} catch (final Exception ex) {
			System.out.println("Error setting testParkingLotExit");
			ex.printStackTrace();
		} finally {
			dataBaseTestConfig.closeConnection(con);
		}

	}
	
	@Test
	// check that a fare with discount price generated is populated correctly in the
	// database
	public void testParkingLotExitRecurringUser() {

		final ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processIncomingVehicle();
		parkingService.processExitingVehicle();
		parkingService.processIncomingVehicle();
		
		

		Connection con = null;
		try {
			con = dataBaseTestConfig.getConnection();
			
			final Date newInTime1 = new Date();
			newInTime1.setTime(System.currentTimeMillis() - 25 * 60 * 1000);
			final PreparedStatement ps0 = con
					.prepareStatement("update ticket set IN_TIME=? where VEHICLE_REG_NUMBER='ABCDEF' AND ID=1");
			ps0.setTimestamp(1, new Timestamp(newInTime1.getTime()));
			ps0.execute();

			dataBaseTestConfig.closePreparedStatement(ps0);
			
			
			final Date newInTime2 = new Date();
			newInTime2.setTime(System.currentTimeMillis() - 60 * 60 * 1000);
			final PreparedStatement ps1 = con
					.prepareStatement("update ticket set IN_TIME=? where VEHICLE_REG_NUMBER='ABCDEF' AND ID=2");
			ps1.setTimestamp(1, new Timestamp(newInTime2.getTime()));
			ps1.execute();

			dataBaseTestConfig.closePreparedStatement(ps1);

			parkingService.processExitingVehicle();

			final PreparedStatement ps2 = con
					.prepareStatement("select price from ticket where vehicle_reg_number='ABCDEF' and id=2");
			ps2.execute();
			final ResultSet rs2 = ps2.executeQuery();
			if (rs2.next()) {
				assertThat(rs2.getDouble(1)).isEqualTo(Fare.CAR_RATE_PER_HOUR*(1-Fare.DISCOUNT_VALUE));

			}
			dataBaseTestConfig.closeResultSet(rs2);

		} catch (final Exception ex) {
			System.out.println("Error setting testParkingLotExitRecurringUser");
			ex.printStackTrace();
		} finally {
			dataBaseTestConfig.closeConnection(con);
		}

	}
	

}
