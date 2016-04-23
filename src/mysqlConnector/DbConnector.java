package mysqlConnector;

import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

import javax.security.auth.login.Configuration;
import javax.sql.CommonDataSource;

import Common.Permission;
import Common.Service;

import java.sql.ResultSet;


/**
 * 
 * @author chensqi
 * All you need is the interface of DbConnector
 */
public class DbConnector {
	
	/**
	 * Check if any dangerous character is in stmt  
	 * @param stmt
	 * @return true if safe.
	 */
	boolean securityCheck(String stmt) {
		String[] dangerChar = new String[] {","
				,"|"
				,"."
				," "
				,"\\"
				,"'"
				,"\""};
		for (int i = 0 ; i < dangerChar.length ; ++i)
			if (stmt.contains(dangerChar[i]))
				return false;
		return true;
	}
	Connection conectionToDB() throws SQLException {
		return DriverManager.getConnection(
				 
				ConnectingConfigurations.getConnectingUrlWithDatabaseName(),
				ConnectingConfigurations.getConnectingUserName(),
				ConnectingConfigurations.getConnectingPassword());
	}
	
	// Connect to remote mysql database
	// See ConnectingConfigurations for details
	public DbConnector() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
		
	}
	/**
	 * Simply test if the connector works well 
	 * @throws SQLException
	 */
	void Test() throws SQLException {
		
		System.out.println("Tests initialiing database....");
		init();
		
		Connection con = this.conectionToDB();
		
		System.out.println("Tests creating tables....");
		Statement stmt = con.createStatement();
		stmt.execute("CREATE TABLE test(id int)");
		stmt.execute("DROP TABLE test");
		
		
		System.out.println("Tests user register....");
		Common.UserInfo user = new Common.UserInfo();
		user.userName = "hehe";
		user.loginName = "hehe";
		user.password = "123123";
		this.inputUserRegister(user);
		
		user = new Common.UserInfo();
		user.userName = "haha";
		user.loginName = "haha";
		user.password = "123qwe";
		this.inputUserRegister(user);
		
		System.out.println("Tests read user infomations....");
		user = this.getUserInfo("haha");
		//System.err.println(user.userId + " " + user.userName);
		
		user = this.getUserInfo("hehe");
		//System.err.println(user.userId + " " + user.userName);
		
		
		this.inputNewService("service0");
		this.inputNewParameterIntoService("service0", "para1", "string");
		this.inputNewParameterIntoService("service0", "para2", "int");
		
		Common.Service service = this.getServiceByName("service0");
		System.err.println(service);
		
		//System.out.println("Tests droping database....");
		//clear();
		
		System.out.println("All testcases done!");
	}
	
	/**
	 * Warning! Clear everything, create database and create all tables we needed
	 */
	public void init() throws SQLException {
		clear();
		Connection TempCon = DriverManager.getConnection(
				ConnectingConfigurations.getConnectingUrl(),
				ConnectingConfigurations.getConnectingUserName(),
				ConnectingConfigurations.getConnectingPassword());

		Statement stmt = TempCon.createStatement();
		stmt.execute("CREATE DATABASE " + ConnectingConfigurations.getConnectingDatabaseName());
		createTables();
	}
	/**
	 * Warning! Delete entire database
	 * 
	 * @throws SQLException
	 */
	public void clear() throws SQLException{
		Connection con = this.conectionToDB();
		Statement stmt = con.createStatement();
		stmt.execute("DROP DATABASE IF EXISTS " + ConnectingConfigurations.getConnectingDatabaseName() );
	}
	/**
	 * Create all the tables according to TableConfigurations
	 * @throws SQLException
	 */
	void createTables() throws SQLException {
		
		Connection con = this.conectionToDB();

		Statement stmt = con.createStatement();
		TableConfigurations.generateAllTables();
		for ( int i = 0 ; i < TableConfigurations.tables.size() ; ++i ) {
			Table t = TableConfigurations.tables.get(i);
			//System.err.println(t.getCreatTableStatement());
			stmt.execute(t.getCreatTableStatement());
		}
	}
	
	/**
	 * Register a new user in database with 0.00 money
	 * @param user Not all fields are needed. loginName, password must be included
	 * @return true if register success, false if this userInfo is invalid
	 * @throws SQLException 
	 */
	public boolean inputUserRegister(Common.UserInfo user) throws SQLException {
		Connection con = this.conectionToDB();

		Statement stmt = con.createStatement();
		
		user.Money = new Integer(0);
		if ( user.priviligeLevel == null )
			user.priviligeLevel = new Integer(2) ;
		
		if ( user.loginName == null || user.password == null ) {
			return false;
		}
		if ( user.userId != null )
			return false;
		/**
		 * Avoid SQL Injection Attack
		 */
		if ( securityCheck(user.loginName) && securityCheck(user.password) ) {
			String s = "INSERT INTO " + TableConfigurations.tableNames[0]
					+ user.getColumnNameStatement()
					+ " VALUES " + user.getValueStatement();
			//System.out.println(s);
			return stmt.execute(s);
		}
		else
			return false;
	}
	/**
	 * When user login, add a log in database for this login
	 * @return true if success
	 */
	public boolean inputUserLogin(String loginName) {
		//@TODO
		return true;
	}
	/**
	 * When user call a service, add a log in database for this calling
	 * @param userName
	 * @param serviceName
	 * @return
	 */
	public boolean inputUserCallService(String loginName, String serviceName) {
		//!TODO
		return true;
	}
	/**
	 * Add a new IP into white list
	 * @return
	 * @throws SQLException 
	 */
	public boolean inputWhiteList(Common.IP ipAddress) throws SQLException {
		
		Connection con = this.conectionToDB();

		Statement stmt = con.createStatement();
		String s = "INSERT INTO " + TableConfigurations.tableNames[5]	//	service table
				+ "(ip)"
				+ " VALUES " + "('" + ipAddress + "')";

		return stmt.execute(s);
		
	}
	/**
	 * Create a new service in db with empty parameter
	 * @param service
	 * @return If success
	 * @throws SQLException 
	 */
	public boolean inputNewService(String serviceName) throws SQLException {
		
		Connection con = this.conectionToDB();
		Statement stmt = con.createStatement();
		String s = "INSERT INTO " + TableConfigurations.tableNames[1]	//	service table
				+ "(serviceName)"
				+ " VALUES " + "('" + serviceName + "')";

		//	Insert a new record into service table
		return stmt.execute(s);
	}
	/**
	 * Insert a new parameter to an existed service
	 * @param para
	 * @return True if success, false if this service dosen't exist
	 * @throws SQLException 
	 */
	public boolean inputNewParameterIntoService( String serviceName, String paraName, String paraType ) throws SQLException {
		Connection con = this.conectionToDB();
		
		Statement stmt = con.createStatement();
		String s = "INSERT INTO " + TableConfigurations.tableNames[4]
				+ "(serviceName,ParaName,ParaType)"
				+ " VALUES " 
				+ "("
				+ "'" + serviceName + "'" + ","
				+ "'" + paraName + "'" + ","
				+ "'" + paraType + "'"
				+ ")";
		
		//	Insert a new record into service table
		return stmt.execute(s);
	}
	/**
	 * Set a user's money into @money
	 * @param loginName
	 * @param money
	 * @return true if success
	 * @throws SQLException
	 */
	public boolean setUserMoney(String loginName, int money) throws SQLException {
		Connection con = this.conectionToDB();
		Statement stmt = con.createStatement();
		String s = " UPDATE " + TableConfigurations.tableNames[0]
				+ " SET Money = " + money
				+ " WHERE loginName = " + "'" + loginName + "'"; 
		return stmt.execute(s);
	}
	public boolean setServiceFee(String serviceName, int cost) throws SQLException {
		Connection con = this.conectionToDB();
		Statement stmt = con.createStatement();
		String s = " UPDATE " + TableConfigurations.tableNames[1]
				+ " SET ServiceFee = " + cost
				+ " WHERE ServiceName = " + "'" + serviceName + "'"; 
		return stmt.execute(s);
	}
	/**
	 * 
	 * @param loginName 
	 * @return information for this user if existed. null if user not found 
	 * @throws SQLException
	 */
	public Common.UserInfo getUserInfo(String loginName) throws SQLException{
		Connection con = this.conectionToDB();

		Statement stmt = con.createStatement();
		ResultSet res =  stmt.executeQuery("SELECT * FROM " 
								+ TableConfigurations.tableNames[0]
								+ " WHERE "
								+ " loginName = " 
								+ "'" + loginName + "'");	//	fetch results from account table
		
		int cnt = 0 ;
		Common.UserInfo user = new Common.UserInfo();
		while ( res.next() ) {
			++cnt;
			
			//System.err.println(res.toString());
			
			user.userId = res.getInt(1);
			user.userName = res.getString(2); //	The real name of user
			user.loginName = res.getString(3);	//	Account name
			user.password = res.getString(4); //	Encrypted password
			user.email = res.getString(5);
			user.companyAddress = res.getString(6);
			user.signUpTime = res.getDate(7);	//	Time when user registered
			user.lastLogInTime = res.getDate(8);
			user.Money = res.getInt(9);	//	Money = 1 <==> 0.01 RMB
			user.priviligeLevel = res.getInt(10);	//	Level 1,2,3.. for managers. Level 0 for users
			user.activeOrNot = res.getBoolean(11); //	0 or 1
		}
		if ( cnt == 0 )
			return null;
		return user;
	}
	/**
	 * 
	 * @param userName
	 * @return a list of logs
	 */
	public ArrayList<Common.UserLog> getUserLog(String longinName) {
		ArrayList<Common.UserLog>res = new ArrayList<Common.UserLog>();
		return res;	//@TODO
	}
	/**
	 * 
	 * @param serviceName
	 * @return a service instance, null if no such service
	 * @throws SQLException 
	 */
	public Common.Service getServiceByName(String serviceName) throws SQLException {
		Connection con = this.conectionToDB();
		Statement stmt = con.createStatement();
		// Fetch serviceIds
		ResultSet res =  stmt.executeQuery("SELECT * FROM " 
								+ TableConfigurations.tableNames[1]
								+ " WHERE "
								+ " serviceName = " 
								+ "'" + serviceName + "'");	
		Common.Service service = new Common.Service(serviceName);
		if ( res.next() ) {
			service.serviceId = res.getInt("serviceId");
			service.serviceFee = res.getInt("serviceFee");
		}
		else
			return null;
		
		// Fetch serviceIds
		res =  stmt.executeQuery("SELECT * FROM " 
										+ TableConfigurations.tableNames[4]
										+ " WHERE "
										+ " serviceName = " 
										+ "'" + serviceName + "'");
		while ( res.next() ) {
			service.addPara(res.getString(2), res.getString(3));
		}
		return service;
	}
	/**
	 * @return Return all the service as an arraylist
	 * @throws SQLException
	 */
	public ArrayList<Common.Service> getAllServices() throws SQLException {
		Connection con = this.conectionToDB();
		Statement stmt = con.createStatement();
		// Fetch serviceIds
		ResultSet res =  stmt.executeQuery("SELECT * FROM " 
								+ TableConfigurations.tableNames[1] );	
		ArrayList<Common.Service> serviceList = new ArrayList<Service>();
		while ( res.next() ) {
			Common.Service service= new Common.Service( res.getString("serviceName") );
			service.serviceId = res.getInt("serviceId");
			service.serviceFee = res.getInt("serviceFee");
			
			serviceList.add(service);
		}
		
		res = stmt.executeQuery("SELECT * FROM " + TableConfigurations.tableNames[4]);
		while ( res.next() ) {
			String serviceName = res.getString("serviceName");
			for ( Common.Service s : serviceList) {
				if ( s.serviceName.equals(serviceName)) {
					s.addPara(res.getString("paraName"), res.getString("paraType"));
					break;
				}
			}
		}
		return serviceList;
	}
	/**
	 * @param ip
	 * @return true if this ip is contained in the white list
	 * @throws SQLException 
	 */
	public boolean containedByWhiteList( Common.IP ip ) throws SQLException {
		Connection con = this.conectionToDB();
		Statement stmt = con.createStatement();
		// Fetch serviceIds
		ResultSet res =  stmt.executeQuery("SELECT * FROM " 
								+ TableConfigurations.tableNames[5]
								+ " WHERE "
								+ " ip = " 
								+ "'" + ip + "'");	
		return res.next();
	}
}

