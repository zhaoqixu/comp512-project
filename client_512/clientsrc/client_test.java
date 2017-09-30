import MidInterface.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RMISecurityManager;

import java.util.*;
import java.io.*;

	
public class client_test
{
	static String message = "blank";
	static MiddleWare mw = null;

	public static void main(String args[])
	{
		client obj = new client();
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		String command = "";
		Vector arguments  = new Vector();
		int Id, Cid;
		int flightNum;
		int flightPrice;
		int flightSeats;
		boolean Room;
		boolean Car;
		int price;
		int numRooms;
		int numCars;
		String location;


		String server = "localhost";
		int port = 1099;
		if (args.length > 0)
		{
			server = args[0];
		}
		if (args.length > 1)
		{
			port = Integer.parseInt(args[1]);
		}
		if (args.length > 2)
		{
			System.out.println ("Usage: java client [rmihost [rmiport]]");
			System.exit(1);
		}
		
		try 
		{
			// get a reference to the rmiregistry
			Registry registry = LocateRegistry.getRegistry(server, port);
			// get the proxy and the remote reference by rmiregistry lookup
			mw = (MiddleWare) registry.lookup("TripMiddleWare");
			if(mw!=null)
			{
				System.out.println("Successful");
				System.out.println("Connected to MW");
			}
			else
			{
				System.out.println("Unsuccessful");
			}
			// make call on remote method
		} 
		catch (Exception e) 
		{    
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
		
		
		
		if (System.getSecurityManager() == null) {
			//System.setSecurityManager(new RMISecurityManager());
		}

		
		System.out.println("\n\n\tClient Interface");
		System.out.println("Type \"help\" for list of supported commands");
		// Add 20 flights with i,i,i,i args
		for (int i = 1; i <= 20; i++) {
			System.out.println("\n>newflight,"+i+","+i+","+i+","+i);
			System.out.println("Adding a new Flight using id: "+i);
			System.out.println("Flight number: "+i);
			System.out.println("Add Flight Seats: "+i);
			System.out.println("Set Flight Price: "+i);
			try{
			if(mw.addFlight(i,i,i,i))
				System.out.println("Flight added");
			else
				System.out.println("Flight could not be added");
			}
			catch(Exception e){
			System.out.println("EXCEPTION:");
			System.out.println(e.getMessage());
			e.printStackTrace();
			}
		}
		// Add 20 Cars with i,i,i,i args
		for (int i = 1; i <= 20; i++) {
		System.out.println("\n>newcar,"+i+","+i+","+i+","+i);
            System.out.println("Adding a new Car using id: "+i);
            System.out.println("Car Location: "+i);
            System.out.println("Add Number of Cars: "+i);
            System.out.println("Set Price: "+i);
            try{
            if(mw.addCars(i,String.valueOf(i),i,i))
                System.out.println("Cars added");
            else
                System.out.println("Cars could not be added");
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// Add 20 Rooms with i,i,i,i args
		for (int i = 1; i <= 20; i++) {
			System.out.println("\n>newroom,"+i+","+i+","+i+","+i);
            System.out.println("Adding a new Room using id: "+i);
            System.out.println("Room Location: "+i);
            System.out.println("Add Number of Rooms: "+i);
            System.out.println("Set Price: "+i);
            try{
            if(mw.addRooms(i,String.valueOf(i),i,i))
                System.out.println("Rooms added");
            else
                System.out.println("Rooms could not be added");
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// Add 5 Customers with given ID with i,i args
		for (int i = 1; i <= 5; i++) {
			System.out.println("\n>newflight,"+i+","+i+","+i+","+i);
			System.out.println("Adding a new Customer using id:"+i + " and cid " +i);
            try{
            boolean customer=mw.newCustomer(i,i);
            System.out.println("new customer id:"+i);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// Add 5 Customers with random ID
		int[] randCust = new int[5];
		for (int i = 1; i<=5 ; i++ ) {
			System.out.println("\n>newcustomer,"+i);
            System.out.println("Adding a new Customer using id:"+i);
            try{
            randCust[i-1]=mw.newCustomer(i);
            System.out.println("new customer id:"+randCust[i-1]);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// Delete Last 5 flights
		for (int i = 16; i<=20; i++) {
			System.out.println("\n>deleteflight,"+i+","+i);
			System.out.println("Deleting a flight using id: "+i);
            System.out.println("Flight Number: "+i);
            try{
            if(mw.deleteFlight(i,i))
                System.out.println("Flight Deleted");
            else
                System.out.println("Flight could not be deleted");
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// Delete Last 5 Cars
		for (int i = 16; i<=20; i++) {
			System.out.println("\n>deletecar,"+i+","+i);
            System.out.println("Deleting the cars from a particular location  using id: "+i);
            System.out.println("Car Location: "+i);
            try{
            if(mw.deleteCars(i,String.valueOf(i)))
                System.out.println("Cars Deleted");
            else
                System.out.println("Cars could not be deleted");
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// Delete Last 5 Rooms
		for (int i = 16; i<=20; i++) {
			System.out.println("\n>deleteroom,"+i+","+i);
            System.out.println("Deleting all rooms from a particular location  using id: "+i);
            System.out.println("Room Location: "+i);
            try{
            if(mw.deleteRooms(i,String.valueOf(i)))
                System.out.println("Rooms Deleted");
            else
                System.out.println("Rooms could not be deleted");
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// Delete Random Customers created before
		for (int i = 1; i<=5 ; i++) {
			System.out.println("\n>deletecustomer,"+i+","+randCust[i-1]);
			System.out.println("Deleting a customer from the database using id: "+i);
            System.out.println("Customer id: "+randCust[i-1]);
            try{
            if(mw.deleteCustomer(i,randCust[i-1]))
                System.out.println("Customer Deleted");
            else
                System.out.println("Customer could not be deleted");
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// Queryflight with existed flightNum, deleted flightNum and non-existed flightNum
		for (int i = 1;i <= 5; i++) {
			System.out.println("\n>queryflight,"+i+","+i);
			System.out.println("Querying a flight using id: "+i);
            System.out.println("Flight number: "+i);
            try{
            int seats=mw.queryFlight(i,i);
            System.out.println("Number of seats available:"+seats);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
            System.out.println("\n>queryflight,"+i+","+(21-i));
            System.out.println("Querying a flight using id: "+i);
            System.out.println("Flight number: "+ (21-i));
            try{
            int seats=mw.queryFlight(i,21-i);
            System.out.println("Number of seats available:"+seats);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
            System.out.println("\n>queryflight,"+i+","+(i*100));
            System.out.println("Querying a flight using id: "+i);
            System.out.println("Flight number: "+(i*100));
            try{
            int seats=mw.queryFlight(i,i*100);
            System.out.println("Number of seats available:"+seats);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// Querycar with existed flightNum, deleted flightNum and non-existed flightNum
		for (int i = 1;i <= 5; i++) {
			System.out.println("\n>querycar,"+i+","+i);
			System.out.println("Querying a car location using id: "+i);
            System.out.println("Car location: "+i);
            try{
            numCars=mw.queryCars(i,String.valueOf(i));
            System.out.println("number of Cars at this location:"+numCars);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
            System.out.println("\n>querycar,"+i+","+(21-i));
            System.out.println("Querying a car location using id: "+i);
            System.out.println("Car location: "+ (21-i));
            try{
            numCars=mw.queryCars(i,String.valueOf(21-i));
            System.out.println("number of Cars at this location:"+numCars);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
            System.out.println("\n>querycar,"+i+","+(i*100));
            System.out.println("Querying a car location using id: "+i);
            System.out.println("Car location: "+(i*100));
            try{
            numCars=mw.queryCars(i,String.valueOf(i*100));
            System.out.println("number of Cars at this location:"+numCars);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// Queryroom with existed flightNum, deleted flightNum and non-existed flightNum
		for (int i = 1;i <= 5; i++) {
			System.out.println("\n>queryroom,"+i+","+i);
			System.out.println("Querying a room location using id: "+i);
            System.out.println("Room location: "+i);
            try{
            numRooms=mw.queryRooms(i,String.valueOf(i));
            System.out.println("number of Rooms at this location:"+numRooms);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
            System.out.println("\n>queryroom,"+i+","+(21-i));
            System.out.println("Querying a room location using id: "+i);
            System.out.println("Room location: "+(21-i));
            try{
            numRooms=mw.queryRooms(i,String.valueOf(21-i));
            System.out.println("number of Rooms at this location:"+numRooms);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
            System.out.println("\n>queryroom,"+i+","+(i*100));
            System.out.println("Querying a room location using id: "+i);
            System.out.println("Room location: "+(i*100));
            try{
            numRooms=mw.queryRooms(i,String.valueOf(i*100));
            System.out.println("number of Rooms at this location:"+numRooms);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// Queryflightprice with existed flightNum, deleted flightNum and non-existed flightNum
		for (int i = 1;i <= 5; i++) {
		System.out.println("\n>queryflightprice,"+i+","+i);
		System.out.println("Querying a flight Price using id: "+i);
            System.out.println("Flight number: "+i);
            try{
            price=mw.queryFlightPrice(i,i);
            System.out.println("Price of a seat:"+price);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
            System.out.println("\n>queryflightprice,"+i+","+(21-i));
            System.out.println("Querying a flight Price using id: "+i);
            System.out.println("Flight number: "+ (21-i));
            try{
            price=mw.queryFlightPrice(i,21-i);
            System.out.println("Price of a seat:"+price);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
            System.out.println("\n>queryflightprice,"+i+","+(i*100));
            System.out.println("Querying a flight Price using id: "+i);
            System.out.println("Flight number: "+(i*100));
            try{
            price=mw.queryFlightPrice(i,i*100);
            System.out.println("Price of a seat:"+price);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// Querycarprice with existed flightNum, deleted flightNum and non-existed flightNum
		for (int i = 1;i <= 5; i++) {
		System.out.println("\n>querycarprice,"+i+","+i);
		System.out.println("Querying a car price using id: "+i);
            System.out.println("Car location: "+i);
            try{
            price=mw.queryCarsPrice(i,String.valueOf(i));
            System.out.println("Price of a car at this location:"+price);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
            System.out.println("\n>querycarprice,"+i+","+(21-i));
            System.out.println("Querying a car price using id: "+i);
            System.out.println("Car location: "+ (21-i));
            try{
            price=mw.queryCarsPrice(i,String.valueOf(21-i));
            System.out.println("Price of a car at this location:"+price);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
            System.out.println("\n>querycarprice,"+i+","+(i*100));
            System.out.println("Querying a car price using id: "+i);
            System.out.println("Car location: "+(i*100));
            try{
            price=mw.queryCarsPrice(i,String.valueOf(i*100));
            System.out.println("Price of a car at this location:"+price);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// Queryroomprice with existed flightNum, deleted flightNum and non-existed flightNum
		for (int i = 1;i <= 5; i++) {
		System.out.println("\n>queryroomprice,"+i+","+i);
		System.out.println("Querying a room price using id: "+i);
            System.out.println("Room location: "+i);
            try{
            price=mw.queryRoomsPrice(i,String.valueOf(i));
            System.out.println("Price of Rooms at this location:"+price);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
            System.out.println("\n>queryroomprice,"+i+","+(21-i));
            System.out.println("Querying a room price using id: "+i);
            System.out.println("Room location: "+(21-i));
            try{
            price=mw.queryRoomsPrice(i,String.valueOf(21-i));
            System.out.println("Price of Rooms at this location:"+price);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
            System.out.println("\n>queryroomprice,"+i+","+(i*100));
            System.out.println("Querying a room price using id: "+i);
            System.out.println("Room location: "+(i*100));
            try{
            price=mw.queryRoomsPrice(i,String.valueOf(i*100));
            System.out.println("Price of Rooms at this location:"+price);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// reserve a flight
		for (int i = 1; i <= 5; i++) {
		System.out.println("\n>reserveflight,"+i+","+i+","+i);
            System.out.println("Reserving a seat on a flight using id: "+i);
            System.out.println("Customer id: "+i);
            System.out.println("Flight number: "+i);
            try{
            if(mw.reserveFlight(i,i,i))
                System.out.println("Flight Reserved");
            else
                System.out.println("Flight could not be reserved.");
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// reserve a car
		for (int i = 1; i <= 5; i++) {
		System.out.println("\n>reservecar,"+i+","+i+","+i);
            System.out.println("Reserving a car at a location using id: "+i);
            System.out.println("Customer id: "+i);
            System.out.println("Location: "+i);
            
            try{
            if(mw.reserveCar(i,i,String.valueOf(i)))
                System.out.println("Car Reserved");
            else
                System.out.println("Car could not be reserved.");
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// reserve a room
		for (int i = 1; i <= 5; i++) {
			System.out.println("\n>reserveroom,"+i+","+i+","+i);
            System.out.println("Reserving a room at a location using id: "+i);
            System.out.println("Customer id: "+i);
            System.out.println("Location: "+i);
            try{
            if(mw.reserveRoom(i,i,String.valueOf(i)))
                System.out.println("Room Reserved");
            else
                System.out.println("Room could not be reserved.");
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		for (int i = 4; i <=5 ; i++) {
			System.out.println("\n>itinerary,"+i+","+i+",");
			for(int j=0;j<6;j++)
            	System.out.print((i+j)+",");
            System.out.println(i+",true,false");
            System.out.println("Reserving an Itinerary using id:"+i);
            System.out.println("Customer id:"+i);
            for(int j=0;j<6;j++)
            System.out.println("Flight number"+(i+j));
            System.out.println("Location for Car/Room booking:"+i);
            System.out.println("Car to book?:"+"true");
            System.out.println("Room to book?:"+"false");
            try{
            Id = i;
            int customer = i;
            Vector flightNumbers = new Vector();
            for(int j=0;j<6;j++)
                flightNumbers.addElement(i+j);
            location = String.valueOf(i);
            Car = true;
            Room = false;
            
            if(mw.itinerary(Id,customer,flightNumbers,location,Car,Room))
                System.out.println("Itinerary Reserved");
            else
                System.out.println("Itinerary could not be reserved.");
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}
		// querycustomer with 1,i
		for (int i = 1; i <= 5 ; i++) {
			System.out.println("\n>querycustomer,"+i+","+i);
			System.out.println("Querying Customer information using id: "+i);
            System.out.println("Customer id: "+i);
            try{
            String bill=mw.queryCustomerInfo(i,i);
            System.out.println("Customer info:"+bill);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
            System.out.println("\n>querycustomer,"+i+","+randCust[i-1]);
            System.out.println("Querying Customer information using id: "+i);
            System.out.println("Customer id: "+randCust[i-1]);
            try{
            String bill=mw.queryCustomerInfo(i,randCust[i-1]);
            System.out.println("Customer info:"+bill);
            }
            catch(Exception e){
            System.out.println("EXCEPTION:");
            System.out.println(e.getMessage());
            e.printStackTrace();
            }
		}


	}
		
	public Vector parse(String command)
	{
	Vector arguments = new Vector();
	StringTokenizer tokenizer = new StringTokenizer(command,",");
	String argument ="";
	while (tokenizer.hasMoreTokens())
		{
		argument = tokenizer.nextToken();
		argument = argument.trim();
		arguments.add(argument);
		}
	return arguments;
	}
	public int findChoice(String argument)
	{
	if (argument.compareToIgnoreCase("help")==0)
		return 1;
	else if(argument.compareToIgnoreCase("newflight")==0)
		return 2;
	else if(argument.compareToIgnoreCase("newcar")==0)
		return 3;
	else if(argument.compareToIgnoreCase("newroom")==0)
		return 4;
	else if(argument.compareToIgnoreCase("newcustomer")==0)
		return 5;
	else if(argument.compareToIgnoreCase("deleteflight")==0)
		return 6;
	else if(argument.compareToIgnoreCase("deletecar")==0)
		return 7;
	else if(argument.compareToIgnoreCase("deleteroom")==0)
		return 8;
	else if(argument.compareToIgnoreCase("deletecustomer")==0)
		return 9;
	else if(argument.compareToIgnoreCase("queryflight")==0)
		return 10;
	else if(argument.compareToIgnoreCase("querycar")==0)
		return 11;
	else if(argument.compareToIgnoreCase("queryroom")==0)
		return 12;
	else if(argument.compareToIgnoreCase("querycustomer")==0)
		return 13;
	else if(argument.compareToIgnoreCase("queryflightprice")==0)
		return 14;
	else if(argument.compareToIgnoreCase("querycarprice")==0)
		return 15;
	else if(argument.compareToIgnoreCase("queryroomprice")==0)
		return 16;
	else if(argument.compareToIgnoreCase("reserveflight")==0)
		return 17;
	else if(argument.compareToIgnoreCase("reservecar")==0)
		return 18;
	else if(argument.compareToIgnoreCase("reserveroom")==0)
		return 19;
	else if(argument.compareToIgnoreCase("itinerary")==0)
		return 20;
	else if (argument.compareToIgnoreCase("quit")==0)
		return 21;
	else if (argument.compareToIgnoreCase("newcustomerid")==0)
		return 22;
	else
		return 666;

	}

	public void listCommands()
	{
	System.out.println("\nWelcome to the client interface provided to test your project.");
	System.out.println("Commands accepted by the interface are:");
	System.out.println("help");
	System.out.println("newflight\nnewcar\nnewroom\nnewcustomer\nnewcusomterid\ndeleteflight\ndeletecar\ndeleteroom");
	System.out.println("deletecustomer\nqueryflight\nquerycar\nqueryroom\nquerycustomer");
	System.out.println("queryflightprice\nquerycarprice\nqueryroomprice");
	System.out.println("reserveflight\nreservecar\nreserveroom\nitinerary");
	System.out.println("nquit");
	System.out.println("\ntype help, <commandname> for detailed info(NOTE the use of comma).");
	}


	public void listSpecific(String command)
	{
	System.out.print("Help on: ");
	switch(findChoice(command))
		{
		case 1:
		System.out.println("Help");
		System.out.println("\nTyping help on the prompt gives a list of all the commands available.");
		System.out.println("Typing help, <commandname> gives details on how to use the particular command.");
		break;

		case 2:  //new flight
		System.out.println("Adding a new Flight.");
		System.out.println("Purpose:");
		System.out.println("\tAdd information about a new flight.");
		System.out.println("\nUsage:");
		System.out.println("\tnewflight,<id>,<flightnumber>,<flightSeats>,<flightprice>");
		break;
		
		case 3:  //new Car
		System.out.println("Adding a new Car.");
		System.out.println("Purpose:");
		System.out.println("\tAdd information about a new car location.");
		System.out.println("\nUsage:");
		System.out.println("\tnewcar,<id>,<location>,<numberofcars>,<pricepercar>");
		break;
		
		case 4:  //new Room
		System.out.println("Adding a new Room.");
		System.out.println("Purpose:");
		System.out.println("\tAdd information about a new room location.");
		System.out.println("\nUsage:");
		System.out.println("\tnewroom,<id>,<location>,<numberofrooms>,<priceperroom>");
		break;
		
		case 5:  //new Customer
		System.out.println("Adding a new Customer.");
		System.out.println("Purpose:");
		System.out.println("\tGet the system to provide a new customer id. (same as adding a new customer)");
		System.out.println("\nUsage:");
		System.out.println("\tnewcustomer,<id>");
		break;
		
		
		case 6: //delete Flight
		System.out.println("Deleting a flight");
		System.out.println("Purpose:");
		System.out.println("\tDelete a flight's information.");
		System.out.println("\nUsage:");
		System.out.println("\tdeleteflight,<id>,<flightnumber>");
		break;
		
		case 7: //delete Car
		System.out.println("Deleting a Car");
		System.out.println("Purpose:");
		System.out.println("\tDelete all cars from a location.");
		System.out.println("\nUsage:");
		System.out.println("\tdeletecar,<id>,<location>,<numCars>");
		break;
		
		case 8: //delete Room
		System.out.println("Deleting a Room");
		System.out.println("\nPurpose:");
		System.out.println("\tDelete all rooms from a location.");
		System.out.println("Usage:");
		System.out.println("\tdeleteroom,<id>,<location>,<numRooms>");
		break;
		
		case 9: //delete Customer
		System.out.println("Deleting a Customer");
		System.out.println("Purpose:");
		System.out.println("\tRemove a customer from the database.");
		System.out.println("\nUsage:");
		System.out.println("\tdeletecustomer,<id>,<customerid>");
		break;
		
		case 10: //querying a flight
		System.out.println("Querying flight.");
		System.out.println("Purpose:");
		System.out.println("\tObtain Seat information about a certain flight.");
		System.out.println("\nUsage:");
		System.out.println("\tqueryflight,<id>,<flightnumber>");
		break;
		
		case 11: //querying a Car Location
		System.out.println("Querying a Car location.");
		System.out.println("Purpose:");
		System.out.println("\tObtain number of cars at a certain car location.");
		System.out.println("\nUsage:");
		System.out.println("\tquerycar,<id>,<location>");        
		break;
		
		case 12: //querying a Room location
		System.out.println("Querying a Room Location.");
		System.out.println("Purpose:");
		System.out.println("\tObtain number of rooms at a certain room location.");
		System.out.println("\nUsage:");
		System.out.println("\tqueryroom,<id>,<location>");        
		break;
		
		case 13: //querying Customer Information
		System.out.println("Querying Customer Information.");
		System.out.println("Purpose:");
		System.out.println("\tObtain information about a customer.");
		System.out.println("\nUsage:");
		System.out.println("\tquerycustomer,<id>,<customerid>");
		break;               
		
		case 14: //querying a flight for price 
		System.out.println("Querying flight.");
		System.out.println("Purpose:");
		System.out.println("\tObtain price information about a certain flight.");
		System.out.println("\nUsage:");
		System.out.println("\tqueryflightprice,<id>,<flightnumber>");
		break;
		
		case 15: //querying a Car Location for price
		System.out.println("Querying a Car location.");
		System.out.println("Purpose:");
		System.out.println("\tObtain price information about a certain car location.");
		System.out.println("\nUsage:");
		System.out.println("\tquerycarprice,<id>,<location>");        
		break;
		
		case 16: //querying a Room location for price
		System.out.println("Querying a Room Location.");
		System.out.println("Purpose:");
		System.out.println("\tObtain price information about a certain room location.");
		System.out.println("\nUsage:");
		System.out.println("\tqueryroomprice,<id>,<location>");        
		break;

		case 17:  //reserve a flight
		System.out.println("Reserving a flight.");
		System.out.println("Purpose:");
		System.out.println("\tReserve a flight for a customer.");
		System.out.println("\nUsage:");
		System.out.println("\treserveflight,<id>,<customerid>,<flightnumber>");
		break;
		
		case 18:  //reserve a car
		System.out.println("Reserving a Car.");
		System.out.println("Purpose:");
		System.out.println("\tReserve a given number of cars for a customer at a particular location.");
		System.out.println("\nUsage:");
		System.out.println("\treservecar,<id>,<customerid>,<location>,<nummberofCars>");
		break;
		
		case 19:  //reserve a room
		System.out.println("Reserving a Room.");
		System.out.println("Purpose:");
		System.out.println("\tReserve a given number of rooms for a customer at a particular location.");
		System.out.println("\nUsage:");
		System.out.println("\treserveroom,<id>,<customerid>,<location>,<nummberofRooms>");
		break;
		
		case 20:  //reserve an Itinerary
		System.out.println("Reserving an Itinerary.");
		System.out.println("Purpose:");
		System.out.println("\tBook one or more flights.Also book zero or more cars/rooms at a location.");
		System.out.println("\nUsage:");
		System.out.println("\titinerary,<id>,<customerid>,<flightnumber1>....<flightnumberN>,<LocationToBookCarsOrRooms>,<NumberOfCars>,<NumberOfRoom>");
		break;
		

		case 21:  //quit the client
		System.out.println("Quitting client.");
		System.out.println("Purpose:");
		System.out.println("\tExit the client application.");
		System.out.println("\nUsage:");
		System.out.println("\tquit");
		break;
		
		case 22:  //new customer with id
			System.out.println("Create new customer providing an id");
			System.out.println("Purpose:");
			System.out.println("\tCreates a new customer with the id provided");
			System.out.println("\nUsage:");
			System.out.println("\tnewcustomerid, <id>, <customerid>");
			break;

		default:
		System.out.println(command);
		System.out.println("The interface does not support this command.");
		break;
		}
	}
	
	public void wrongNumber() {
	System.out.println("The number of arguments provided in this command are wrong.");
	System.out.println("Type help, <commandname> to check usage of this command.");
	}



	public int getInt(Object temp) throws Exception {
	try {
		return (new Integer((String)temp)).intValue();
		}
	catch(Exception e) {
		throw e;
		}
	}
	
	public boolean getBoolean(Object temp) throws Exception {
		try {
			return (new Boolean((String)temp)).booleanValue();
			}
		catch(Exception e) {
			throw e;
			}
	}

	public String getString(Object temp) throws Exception {
	try {    
		return (String)temp;
		}
	catch (Exception e) {
		throw e;
		}
	}
}