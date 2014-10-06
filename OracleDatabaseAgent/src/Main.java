import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;

class AtributiStupca
{
	public String naziv;
	public String tip;
	
	public AtributiStupca(String naziv, String tip)
	{
		this.naziv = naziv;
		this.tip = tip;
	}
}

public class Main
{
	private static String host = null;
	private static String port = null;
	private static String SID = null;
	private static String username = null;
	private static String password = null;
	private static String columnDelimiter = null;
	private static String columnIDType = null;
	private static String columnNameID = null;
	private static String columnNameIDWhere = null;
	private static String columnNameTime = null;
	private static String checkTableRotation = null;
	private static String sqlSelect = null;
	private static boolean writeLogTime = true;
	private static byte[] tajniKljuc = new byte[]{-171, 116, 19, -25, -28, 31, -143, 58, 63, 67, 71, -80, -11, 21, -130, 143};

	private static Connection vezaNaBazu = null;
	
	public static void Error(String opisGreske) throws IOException
	{
		BufferedWriter pisac = new BufferedWriter(new FileWriter("oracle_database_agent.log", true));
		Calendar trenutniDatum = Calendar.getInstance();
		SimpleDateFormat formatDatuma = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String datumSada = formatDatuma.format(trenutniDatum.getTime());
		pisac.write(datumSada + " oracle_database_agent: " + opisGreske);
		pisac.newLine();
		pisac.close();
		try
		{
			vezaNaBazu.close();
		}
		catch (Exception e) { }
		System.exit(1);
	}
	
	public static String UcitajEnkriptiranuKonfiguraciju() throws IOException
	{
		File ulaznaDatoteka = new File("config");
		String enkriptiranaKonfiguracija = null;
		if(!ulaznaDatoteka.exists())
		{
			Error("configuration file does not exist");
		}
		else
		{
			try
			{
				BufferedReader citac = new BufferedReader(new FileReader(ulaznaDatoteka));
				StringBuilder enkriptiranaKonfiguracijaBuilder = new StringBuilder();
				String linija = null;
				while((linija = citac.readLine()) != null)
				{
					enkriptiranaKonfiguracijaBuilder.append(linija);
				}
				citac.close();
				enkriptiranaKonfiguracija = enkriptiranaKonfiguracijaBuilder.toString();
			}
			catch(Exception e)
			{
				Error("error reading configuration file. Error message: " + e.getMessage());
			}
		}
		return enkriptiranaKonfiguracija;
	}
	
	public static byte[] HexToBytes(String ulaz)
	{
		byte[] bytes = new byte[ulaz.length() / 2];
		for (int i = 0; i < bytes.length; ++i)
			bytes[i] = (byte) Integer.parseInt(ulaz.substring(2 * i, (2 * i) + 2), 16);
		return bytes;
	}
	
	public static String BytesToHex(byte[] bytes)
	{
		StringBuffer stringBuffer = new StringBuffer(bytes.length * 2);
		for (int i = 0; i < bytes.length; ++i)
		{
			stringBuffer.append(Character.forDigit((bytes[i] >> 4) & 0xF, 16));
			stringBuffer.append(Character.forDigit(bytes[i] & 0xF, 16));
		}
		return stringBuffer.toString();
	}
	
	public static void ProvjeriParametreUlazneKonfiguracije() throws IOException
	{
		if (host == null)
		{
			Error("configuration file missing host parameter");
		}
		else if (port == null)
		{
			Error("configuration file missing port parameter");
		}
		else if (SID == null)
		{
			Error("configuration file missing SID parameter");
		}
		else if (columnDelimiter == null)
		{
			Error("configuration file missing columnDelimiter parameter");
		}
		else if (columnIDType == null)
		{
			Error("configuration file missing columnIDType parameter");
		}
		else if (columnNameID == null)
		{
			Error("configuration file missing columnNameID parameter");
		}
		else if (columnNameIDWhere == null)
		{
			Error("configuration file missing columnNameIDWhere parameter");
		}
		else if (checkTableRotation == null)
		{
			Error("configuration file missing checkTableRotation parameter");
		}
		else if (sqlSelect == null)
		{
			Error("configuration file missing sqlSelect parameter");
		}
		if (!sqlSelect.contains("$substitute$"))
		{
			Error("parameter sqlSelect does not contain $substitute$ string");
		}
		if (!(columnIDType.equalsIgnoreCase("number") || columnIDType.equalsIgnoreCase("time") || columnIDType.equalsIgnoreCase("rowid")))
		{
			Error("invalid value for parameter columnIDType in configuration file. Valid values: number, time or rowid");
		}
		if (!(checkTableRotation.equalsIgnoreCase("true") || checkTableRotation.equalsIgnoreCase("false")))
		{
			Error("invalid value for parameter checkTableRotation in configuration file. Valid values: true, false");
		}
	}
	
	public static void ParsirajEnkriptiranuKonfiguraciju(String enkriptiranaKonfiguracija) throws IOException
	{
		Cipher cipher = null;
		try
		{
			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(tajniKljuc, "AES"));
		}
		catch (Exception e)
		{
			Error("error in initializing chiper. Error message: " + e.getMessage());
		}
		try
		{
			String konfiguracija = new String(cipher.doFinal(HexToBytes(enkriptiranaKonfiguracija)));
			Pattern konfiguracijaPattern = Pattern.compile("^([^s][^=]+)=(.+)$", Pattern.MULTILINE);
			Matcher matcher = konfiguracijaPattern.matcher(konfiguracija);
			while (matcher.find())
			{
				String parametar = matcher.group(1);
				if (parametar.equalsIgnoreCase("host"))
				{
					host = matcher.group(2);
				}
				else if (parametar.equalsIgnoreCase("port"))
				{
					port = matcher.group(2); 
				}
				else if (parametar.equalsIgnoreCase("SID"))
				{
					SID = matcher.group(2); 
				}
				else if (parametar.equalsIgnoreCase("username"))
				{
					username = matcher.group(2); 
				}
				else if (parametar.equalsIgnoreCase("password"))
				{
					password = matcher.group(2); 
				}
				else if (parametar.equalsIgnoreCase("columnDelimiter"))
				{
					columnDelimiter = matcher.group(2);
				}
				else if (parametar.equalsIgnoreCase("columnIDType"))
				{
					columnIDType = matcher.group(2);
				}
				else if (parametar.equalsIgnoreCase("columnNameID"))
				{
					columnNameID = matcher.group(2);
				}
				else if (parametar.equalsIgnoreCase("columnNameIDWhere"))
				{
					columnNameIDWhere = matcher.group(2);
				}
				else if (parametar.equalsIgnoreCase("columnNameTime"))
				{
					columnNameTime = matcher.group(2);
				}
				else if (parametar.equalsIgnoreCase("checkTableRotation"))
				{
					checkTableRotation = matcher.group(2);
				}
			}
			konfiguracijaPattern = Pattern.compile("^sqlSelect=([\\s\\S]+)", Pattern.MULTILINE);
			matcher = konfiguracijaPattern.matcher(konfiguracija);
			if (matcher.find())
			{
				String sqlSelectParsiran = matcher.group(1);
				sqlSelectParsiran = sqlSelectParsiran.replaceAll("\r", " ");
				sqlSelectParsiran = sqlSelectParsiran.replaceAll("\n", " ");
				sqlSelectParsiran = sqlSelectParsiran.replaceAll("\t", " ");
				sqlSelectParsiran = sqlSelectParsiran.trim();
				sqlSelect = sqlSelectParsiran;
			}
			ProvjeriParametreUlazneKonfiguracije();
			if (columnNameTime == null || columnNameID.equals(columnNameTime))
			{
				writeLogTime = false;
			}
		}
		catch(Exception e)
		{
			Error("error parsing configuration file. Error message: " + e.getMessage());
		}
	}
	
	public static BigDecimal UcitajPohranjenoStanjeID() throws IOException
	{
		BigDecimal zadnjiProcitaniID = null;
		File ulaznaDatoteka = new File("state");
		if(ulaznaDatoteka.exists())
		{
			String zadnjiProcitaniIDString = null;
			try
			{
				BufferedReader citac = new BufferedReader(new FileReader(ulaznaDatoteka));
				zadnjiProcitaniIDString= citac.readLine();
				citac.close();
			}
			catch(Exception e)
			{
				Error("error reading saved state. Error message: " + e.getMessage());
			}
			try
			{
				zadnjiProcitaniID = new BigDecimal(zadnjiProcitaniIDString);
			}
			catch(Exception e)
			{
				Error("invalid saved state value. Error message: " + e.getMessage());
			}
			return zadnjiProcitaniID;
		}
		zadnjiProcitaniID = new BigDecimal("-1");
		return zadnjiProcitaniID;
	}
	
	public static Timestamp UcitajPohranjenoStanjeVrijeme() throws IOException
	{
		Timestamp zadnjiProcitaniDatum = null;
		File ulaznaDatoteka = new File("state");
		if(ulaznaDatoteka.exists())
		{
			String zadnjiProcitaniDatumString = null;
			try
			{
				BufferedReader citac = new BufferedReader(new FileReader(ulaznaDatoteka));
				zadnjiProcitaniDatumString= citac.readLine();
				citac.close();
			}
			catch(Exception e)
			{
				Error("error reading saved state. Error message: " + e.getMessage());
			}
			try
			{
				zadnjiProcitaniDatum = Timestamp.valueOf(zadnjiProcitaniDatumString);
			}
			catch(Exception e)
			{
				Error("invalid saved state value. Error message: " + e.getMessage());
			}
			return zadnjiProcitaniDatum;
		}
		zadnjiProcitaniDatum = Timestamp.valueOf("1900-01-01 00:00:00.000000");
		return zadnjiProcitaniDatum;
	}
	
	public static String UcitajPohranjenoStanjeRowID() throws IOException
	{
		File ulaznaDatoteka = new File("state");
		String zadnjiProcitaniRowID = null;
		if(ulaznaDatoteka.exists())
		{
			try
			{
				BufferedReader citac = new BufferedReader(new FileReader(ulaznaDatoteka));
				zadnjiProcitaniRowID = citac.readLine();
				citac.close();
			}
			catch(Exception e)
			{
				Error("error reading saved state. Error message: " + e.getMessage());
			}
			return zadnjiProcitaniRowID;
		}
		zadnjiProcitaniRowID = "AAAAAAAAAAAAAAAAAA";
		return zadnjiProcitaniRowID;
	}
	
	public static void PohraniStanjeID(BigDecimal najveciProcitaniID) throws IOException
	{
		try
		{
			String najveciProcitaniIDString = najveciProcitaniID.toString();
			BufferedWriter pisac = new BufferedWriter(new FileWriter("state", false));
			pisac.write(najveciProcitaniIDString);
			pisac.newLine();
			pisac.close();
		}
		catch(Exception e)
		{
			Error("error writing new state. Error message: " + e.getMessage());
		}
	}
	
	public static void PohraniStanjeVrijeme(Timestamp najveciProcitaniDatum) throws IOException
	{
		try
		{
			String najveciProcitaniDatumString = najveciProcitaniDatum.toString();
			BufferedWriter pisac = new BufferedWriter(new FileWriter("state", false));
			pisac.write(najveciProcitaniDatumString);
			pisac.newLine();
			pisac.close();
		}
		catch(Exception e)
		{
			Error("error writing new state. Error message: " + e.getMessage());
		}
	}
	
	public static void PohraniStanjeRowID(String najveciProcitaniRowID) throws IOException
	{
		try
		{
			BufferedWriter pisac = new BufferedWriter(new FileWriter("state", false));
			pisac.write(najveciProcitaniRowID);
			pisac.newLine();
			pisac.close();
		}
		catch(Exception e)
		{
			Error("error writing new state. Error message: " + e.getMessage());
		}
	}
	
	public static void SpojiSeNaOracleBazu() throws IOException
	{
		try
		{
			Class.forName("oracle.jdbc.driver.OracleDriver");
		}
		catch (Exception e)
		{
			Error("can not load JDBC driver. Error message: " + e.getMessage());
		}
		
		try
		{
			String url = "jdbc:oracle:thin:@" + host + ":" + port + ":" + SID;
			vezaNaBazu =  DriverManager.getConnection(url, username, password);
		}
		catch (Exception e)
		{
			Error("can not connect to database: " + SID + ", port: " + port + ", host: " + host + ". Error message: " + e.getMessage());
		}
	}
	
	public static void ZatvoriVezuNaOracleBazi() throws IOException
	{
		try
		{
			vezaNaBazu.close();
		}
		catch (Exception e)
		{
			Error("can not close connection on database: " + SID + ", port: " + port + ", host: " + host + ". Error message: " + e.getMessage());
		}
	}
	
	public static ArrayList<AtributiStupca> DohvatiAtributeStupacaID(ResultSet rezultat) throws SQLException, IOException
	{
		ResultSetMetaData rezultatMetaData = rezultat.getMetaData();
	    int brojStupaca = rezultatMetaData.getColumnCount();
 
	    int idPoljeIndeks = -1;

	    String nazivStupca, tipStupca;
	    ArrayList<AtributiStupca> atributiStupaca = new ArrayList<AtributiStupca>();
	    for (int i = 1; i <= brojStupaca; i++)
	    {
	    	nazivStupca = rezultatMetaData.getColumnName(i);
	    	if(nazivStupca.equalsIgnoreCase(columnNameID))
	    	{
	   			tipStupca = rezultatMetaData.getColumnTypeName(i).toUpperCase();
	   			atributiStupaca.add(new AtributiStupca(nazivStupca, tipStupca));
	   			idPoljeIndeks = i;
	   			break;
	   		}
    	}
    	if (idPoljeIndeks == -1)
    	{
    		Error("ID field: " + columnNameID + " does not exist");
    	}

	    for (int i = 1; i <= brojStupaca; i++)
	    {
	    	if (i == idPoljeIndeks) continue;
	    	nazivStupca = rezultatMetaData.getColumnName(i);
	    	tipStupca = rezultatMetaData.getColumnTypeName(i).toUpperCase();
	    	atributiStupaca.add(new AtributiStupca(nazivStupca, tipStupca));
	    }
	    
	    return atributiStupaca;
	}
	
	public static ArrayList<AtributiStupca> DohvatiAtributeStupacaIDVrijeme(ResultSet rezultat) throws SQLException, IOException
	{
		ResultSetMetaData rezultatMetaData = rezultat.getMetaData();
	    int brojStupaca = rezultatMetaData.getColumnCount();
	    
	    int idPoljeIndeks = -1, vrijemePoljeIndeks = -1;
	    	
	    AtributiStupca idPolje = null;
	    AtributiStupca vrijemePolje = null;
	    String nazivStupca, tipStupca;
	    for (int i = 1; i <= brojStupaca; i++)
	    {
	    	nazivStupca = rezultatMetaData.getColumnName(i);
	    	if(nazivStupca.equalsIgnoreCase(columnNameID))
	    	{
	   			tipStupca = rezultatMetaData.getColumnTypeName(i).toUpperCase();
	   			idPolje = new AtributiStupca(nazivStupca, tipStupca);
	   			idPoljeIndeks = i;
	   		}
	   		else if(nazivStupca.equalsIgnoreCase(columnNameTime))
	   		{
	   			tipStupca = rezultatMetaData.getColumnTypeName(i).toUpperCase();
	   			vrijemePolje = new AtributiStupca(nazivStupca, tipStupca);
	   			vrijemePoljeIndeks = i;
    		}
    	}
    	if (idPoljeIndeks == -1)
    	{
    		Error("ID field: " + columnNameID + " does not exist");
    	}
    	if (vrijemePoljeIndeks == -1)
    	{
    		Error("Time field: " + columnNameTime + " does not exist");
    	}
    	
    	ArrayList<AtributiStupca> atributiStupaca = new ArrayList<AtributiStupca>();
    	atributiStupaca.add(vrijemePolje);
    	atributiStupaca.add(idPolje);
	    for (int i = 1; i <= brojStupaca; i++)
	    {
	    	if (i == idPoljeIndeks || i == vrijemePoljeIndeks) continue;
	    	nazivStupca = rezultatMetaData.getColumnName(i);
	    	tipStupca = rezultatMetaData.getColumnTypeName(i);
	    	atributiStupaca.add(new AtributiStupca(nazivStupca, tipStupca));
	    }
	    
	    return atributiStupaca;
	}

	public static String TransformirajStringZaIspis(String ulazniString)
	{
		ulazniString = ulazniString.replace("\r", " ");
		ulazniString = ulazniString.replace("\n", " ");
		ulazniString = ulazniString.replace("\t", " ");
		ulazniString = ulazniString.trim();
		return ulazniString;
	}
	
	public static BigDecimal OdrediPocetniID(BigDecimal zadnjiProcitaniID) throws IOException, SQLException
	{
		String sqlSelectZamijena = columnNameIDWhere + " = '" + zadnjiProcitaniID.toString() + "'";
		String sqlSelectParsiran = sqlSelect.replace("$substitute$", sqlSelectZamijena);
		ResultSet rezultat = null;
		Statement sqlSelectNaredba = null;
		try
		{
			sqlSelectNaredba = vezaNaBazu.createStatement();
			rezultat = sqlSelectNaredba.executeQuery(sqlSelectParsiran);
		}
		catch (Exception e)
		{
			Error("error executing sql query for checking table rotation: " + sqlSelectParsiran + ". Error message: " + e.getMessage());
		}
		
		try
		{
			if(!rezultat.next())
			{
				zadnjiProcitaniID = new BigDecimal("-1");
			}
		}
		catch(Exception e)
		{
			Error("error reading results from sql query for checking table rotation: " + sqlSelectParsiran + ". Error message: " + e.getMessage());
		}
		
		try
		{
			rezultat.close();
		}
		catch (Exception e)
		{
			Error("error closing sql query reader. Error message: " + e.getMessage());
		}
		
		return zadnjiProcitaniID;
	}

	public static String OdrediPocetniRowID(String zadnjiProcitaniRowID) throws IOException, SQLException
	{
		String sqlSelectZamijena = columnNameIDWhere + " = '" + zadnjiProcitaniRowID + "'";
		String sqlSelectParsiran = sqlSelect.replace("$substitute$", sqlSelectZamijena);
		ResultSet rezultat = null;
		Statement sqlSelectNaredba = null;
		try
		{
			sqlSelectNaredba = vezaNaBazu.createStatement();
			rezultat = sqlSelectNaredba.executeQuery(sqlSelectParsiran);
		}
		catch (Exception e)
		{
			Error("error executing sql query for checking table rotation: " + sqlSelectParsiran + ". Error message: " + e.getMessage());
		}
		
		try
		{
			if(!rezultat.next())
			{
				zadnjiProcitaniRowID = "AAAAAAAAAAAAAAAAAA";
			}
		}
		catch(Exception e)
		{
			Error("error reading results from sql query for checking table rotation: " + sqlSelectParsiran + ". Error message: " + e.getMessage());
		}
		
		try
		{
			rezultat.close();
		}
		catch (Exception e)
		{
			Error("error closing sql query reader. Error message: " + e.getMessage());
		}
		
		return zadnjiProcitaniRowID;
	}
	
	public static BigDecimal DohvatiNoveZapiseID(BigDecimal zadnjiProcitaniID) throws IOException, SQLException
	{
		String sqlSelectZamijena = columnNameIDWhere + " > '" + zadnjiProcitaniID.toString() + "'";
		String sqlSelectParsiran = sqlSelect.replace("$substitute$", sqlSelectZamijena);
		ResultSet rezultat = null;
		Statement sqlSelectNaredba = null;
		try
		{
			sqlSelectNaredba = vezaNaBazu.createStatement();
			rezultat = sqlSelectNaredba.executeQuery(sqlSelectParsiran);
		}
		catch (Exception e)
		{
			Error("error executing sql query: " + sqlSelectParsiran + ". Error message: " + e.getMessage());
		}
		
		ArrayList<AtributiStupca> atributiStupaca = null;
		if (writeLogTime)
		{
			atributiStupaca = DohvatiAtributeStupacaIDVrijeme(rezultat);
		}
		else
		{
			atributiStupaca = DohvatiAtributeStupacaID(rezultat);
		}

		try
		{
			while (rezultat.next())
			{
				StringBuilder redak = new StringBuilder();
				for (int i = 0; i < atributiStupaca.size(); i++)
				{
					try
					{
						String vrijednost = null;
						AtributiStupca atributStupca = atributiStupaca.get(i);
						if (atributStupca.tip.equalsIgnoreCase("TIMESTAMP"))
						{
							vrijednost = rezultat.getTimestamp(atributStupca.naziv).toString();
						}
						else if (atributStupca.tip.equalsIgnoreCase("TIME"))
						{
							vrijednost = rezultat.getTime(atributStupca.naziv).toString();
						}
						else if (atributStupca.tip.equalsIgnoreCase("DATE"))
						{
							vrijednost = rezultat.getDate(atributStupca.naziv).toString();
						}
						else if (atributStupca.tip.equalsIgnoreCase("BINARY") || atributStupca.tip.equalsIgnoreCase("VARBINARY") || atributStupca.tip.equalsIgnoreCase("VARBINARY") || atributStupca.tip.equalsIgnoreCase("LONGVARBINARY") || atributStupca.tip.equalsIgnoreCase("RAW"))
						{
							vrijednost = BytesToHex(rezultat.getBytes(atributStupca.naziv));
						}
						else
						{
							vrijednost = rezultat.getObject(atributStupca.naziv).toString();
							vrijednost = TransformirajStringZaIspis(vrijednost);
						}
						redak.append(atributStupca.naziv);
						redak.append(": ");
						redak.append(vrijednost);
						redak.append(columnDelimiter);
					}
					catch (Exception e) { }
				}
				BigDecimal procitaniID = new BigDecimal(rezultat.getObject(columnNameID).toString());
				if (procitaniID.compareTo(zadnjiProcitaniID) > 0)
				{
					zadnjiProcitaniID = procitaniID;
				}
				int velicinaRedka = redak.length();
				redak.delete(velicinaRedka - columnDelimiter.length(), velicinaRedka);
				System.out.println(redak.toString());
			}
		}
		catch (Exception e)
		{
			Error("error fetching data from database. Error message: " + e.getMessage());
		}
		
		try
		{
			rezultat.close();
		}
		catch (Exception e)
		{
			Error("error closing sql query reader. Error message: " + e.getMessage());
		}
		
		return zadnjiProcitaniID;
	}
	
	public static Timestamp DohvatiNoveZapiseVrijeme(Timestamp zadnjiProcitaniDatum) throws IOException, SQLException
	{
		String sqlSelectZamijena = columnNameIDWhere + " > TO_TIMESTAMP('" + zadnjiProcitaniDatum.toString() + "', 'YYYY-MM-DD HH24:MI:SS.FF6')";
		String sqlSelectParsiran = sqlSelect.replace("$substitute$", sqlSelectZamijena);
		ResultSet rezultat = null;
		Statement sqlSelectNaredba = null;
		try
		{
			sqlSelectNaredba = vezaNaBazu.createStatement();
			rezultat = sqlSelectNaredba.executeQuery(sqlSelectParsiran);
		}
		catch (Exception e)
		{
			Error("error executing sql query: " + sqlSelectParsiran + ". Error message: " + e.getMessage());
		}
		
		ArrayList<AtributiStupca> atributiStupaca = null;
		if (writeLogTime)
		{
			atributiStupaca = DohvatiAtributeStupacaIDVrijeme(rezultat);
		}
		else
		{
			atributiStupaca = DohvatiAtributeStupacaID(rezultat);
		}
		
		long najveciProcitaniDatum = zadnjiProcitaniDatum.getTime();
		try
		{
			while (rezultat.next())
			{
				StringBuilder redak = new StringBuilder();
				for (int i = 0; i < atributiStupaca.size(); i++)
				{
					try
					{
						String vrijednost = null;
						AtributiStupca atributStupca = atributiStupaca.get(i);
						if (atributStupca.tip.equalsIgnoreCase("TIMESTAMP"))
						{
							vrijednost = rezultat.getTimestamp(atributStupca.naziv).toString();
						}
						else if (atributStupca.tip.equalsIgnoreCase("TIME"))
						{
							vrijednost = rezultat.getTime(atributStupca.naziv).toString();
						}
						else if (atributStupca.tip.equalsIgnoreCase("DATE"))
						{
							vrijednost = rezultat.getDate(atributStupca.naziv).toString();
						}
						else if (atributStupca.tip.equalsIgnoreCase("BINARY") || atributStupca.tip.equalsIgnoreCase("VARBINARY") || atributStupca.tip.equalsIgnoreCase("VARBINARY") || atributStupca.tip.equalsIgnoreCase("LONGVARBINARY") || atributStupca.tip.equalsIgnoreCase("RAW"))
						{
							vrijednost = BytesToHex(rezultat.getBytes(atributStupca.naziv));
						}
						else
						{
							vrijednost = rezultat.getObject(atributStupca.naziv).toString();
							vrijednost = TransformirajStringZaIspis(vrijednost);
						}
						redak.append(atributStupca.naziv);
						redak.append(": ");
						redak.append(vrijednost);
						redak.append(columnDelimiter);
					}
					catch (Exception e) { }
				}
				Timestamp procitaniDatum = rezultat.getTimestamp(columnNameID);
				long procitaniDatumLong = procitaniDatum.getTime();
				if (procitaniDatumLong > najveciProcitaniDatum)
				{
					zadnjiProcitaniDatum = procitaniDatum;
					najveciProcitaniDatum = procitaniDatumLong;
				}
				int velicinaRedka = redak.length();
				redak.delete(velicinaRedka - columnDelimiter.length(), velicinaRedka);
				System.out.println(redak.toString());
			}
		}
		catch (Exception e)
		{
			Error("error fetching data from database. Error message: " + e.getMessage());
		}
		
		try
		{
			rezultat.close();
		}
		catch (Exception e)
		{
			Error("error closing sql query reader. Error message: " + e.getMessage());
		}
		
		return zadnjiProcitaniDatum;
	}
	
	public static String DohvatiNoveZapiseRowID(String zadnjiProcitaniRowID)  throws IOException, SQLException
	{
		String sqlSelectZamijena = columnNameIDWhere + " > '" + zadnjiProcitaniRowID + "'";
		String sqlSelectParsiran = sqlSelect.replace("$substitute$", sqlSelectZamijena);
		ResultSet rezultat = null;
		Statement sqlSelectNaredba = null;
		try
		{
			sqlSelectNaredba = vezaNaBazu.createStatement();
			rezultat = sqlSelectNaredba.executeQuery(sqlSelectParsiran);
		}
		catch (Exception e)
		{
			Error("error executing sql query: " + sqlSelectParsiran + ". Error message: " + e.getMessage());
		}
		
		ArrayList<AtributiStupca> atributiStupaca = null;
		if (writeLogTime)
		{
			atributiStupaca = DohvatiAtributeStupacaIDVrijeme(rezultat);
		}
		else
		{
			atributiStupaca = DohvatiAtributeStupacaID(rezultat);
		}

		try
		{
			while (rezultat.next())
			{
				StringBuilder redak = new StringBuilder();
				for (int i = 0; i < atributiStupaca.size(); i++)
				{
					try
					{
						String vrijednost = null;
						AtributiStupca atributStupca = atributiStupaca.get(i);
						if (atributStupca.tip.equalsIgnoreCase("TIMESTAMP"))
						{
							vrijednost = rezultat.getTimestamp(atributStupca.naziv).toString();
						}
						else if (atributStupca.tip.equalsIgnoreCase("TIME"))
						{
							vrijednost = rezultat.getTime(atributStupca.naziv).toString();
						}
						else if (atributStupca.tip.equalsIgnoreCase("DATE"))
						{
							vrijednost = rezultat.getDate(atributStupca.naziv).toString();
						}
						else if (atributStupca.tip.equalsIgnoreCase("BINARY") || atributStupca.tip.equalsIgnoreCase("VARBINARY") || atributStupca.tip.equalsIgnoreCase("VARBINARY") || atributStupca.tip.equalsIgnoreCase("LONGVARBINARY") || atributStupca.tip.equalsIgnoreCase("RAW"))
						{
							vrijednost = BytesToHex(rezultat.getBytes(atributStupca.naziv));
						}
						else if (atributStupca.tip.equalsIgnoreCase("ROWID"))
						{
							vrijednost = rezultat.getString(columnNameID);
						}
						else
						{
							vrijednost = rezultat.getObject(atributStupca.naziv).toString();
							vrijednost = TransformirajStringZaIspis(vrijednost);
						}
						redak.append(atributStupca.naziv);
						redak.append(": ");
						redak.append(vrijednost);
						redak.append(columnDelimiter);
					}
					catch (Exception e) { }
				}
				zadnjiProcitaniRowID = rezultat.getString(columnNameID);
				int velicinaRedka = redak.length();
				redak.delete(velicinaRedka - columnDelimiter.length(), velicinaRedka);
				System.out.println(redak.toString());
			}
		}
		catch (Exception e)
		{
			Error("error fetching data from database. Error message: " + e.getMessage());
		}
		
		try
		{
			rezultat.close();
		}
		catch (Exception e)
		{
			Error("error closing sql query reader. Error message: " + e.getMessage());
		}
		
		return zadnjiProcitaniRowID;
	}
	
	public static void main(String[] args) throws IOException, SQLException
	{
		String enkriptiranaKonfiguracija = UcitajEnkriptiranuKonfiguraciju();
		ParsirajEnkriptiranuKonfiguraciju(enkriptiranaKonfiguracija);
		if (columnIDType.equalsIgnoreCase("number"))
		{
			BigDecimal zadnjiProcitaniID = UcitajPohranjenoStanjeID();
			SpojiSeNaOracleBazu();
			BigDecimal najveciProcitaniID = null;
			if (checkTableRotation == "true")
			{
				BigDecimal pocetniID = OdrediPocetniID(zadnjiProcitaniID);
				najveciProcitaniID = DohvatiNoveZapiseID(pocetniID);
			}
			else najveciProcitaniID = DohvatiNoveZapiseID(zadnjiProcitaniID);
			PohraniStanjeID(najveciProcitaniID);
			ZatvoriVezuNaOracleBazi();
		}
		else if (columnIDType.equalsIgnoreCase("time"))
		{
			Timestamp zadnjiProcitaniDatum = UcitajPohranjenoStanjeVrijeme();
			SpojiSeNaOracleBazu();
			Timestamp najveciProcitaniDatum = DohvatiNoveZapiseVrijeme(zadnjiProcitaniDatum);
			PohraniStanjeVrijeme(najveciProcitaniDatum);
			ZatvoriVezuNaOracleBazi();
		}
		else
		{
			String zadnjiProcitaniRowID = UcitajPohranjenoStanjeRowID();
			SpojiSeNaOracleBazu();
			String najveciProcitaniRowID = null;
			if (checkTableRotation == "true")
			{
				String pocetniRowID = OdrediPocetniRowID(zadnjiProcitaniRowID);
				najveciProcitaniRowID = DohvatiNoveZapiseRowID(pocetniRowID);
			}
			else najveciProcitaniRowID = DohvatiNoveZapiseRowID(zadnjiProcitaniRowID);
			PohraniStanjeRowID(najveciProcitaniRowID);
			ZatvoriVezuNaOracleBazi();
		}
	}
}
