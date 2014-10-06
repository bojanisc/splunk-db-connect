import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class Main
{
	private static byte[] tajniKljuc = new byte[]{-171, 116, 19, -25, -28, 31, -143, 58, 63, 67, 71, -80, -11, 21, -130, 143};
	
	public static void Error(String opisGreske)
	{
		System.out.println("Oracle database agent encryptor: " + opisGreske);
		System.exit(1);
	}
    
	public static String UcitajKonfiguraciju() throws IOException
	{
		File ulaznaDatoteka = new File("input_config");
		if(!ulaznaDatoteka.exists())
		{
			Error("input configuration file does not exist");
		}
		else
		{
			try
			{
				BufferedReader citac = new BufferedReader(new FileReader(ulaznaDatoteka));
				StringBuilder konfiguracijaBuilder = new StringBuilder();
				String linija = null;
				while((linija = citac.readLine()) != null)
				{
					konfiguracijaBuilder.append(linija);
					konfiguracijaBuilder.append("\n");
				}
				citac.close();
				String konfiguracija =  konfiguracijaBuilder.toString();
				return konfiguracija;
			}
			catch(Exception e)
			{
				Error("error reading configuration file");
			}
		}
		return null;
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
	
	public static String EnkriptirajKonfiguraciju(String konfiguracija)
	{
		Cipher cipher = null;
		try
		{
			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(tajniKljuc, "AES"));
		}
		catch (Exception e)
		{
			Error("error in initializing chiper");
		}
		byte[] enkriptiranaKonfiguracija = null;
		try
		{
			enkriptiranaKonfiguracija = cipher.doFinal(konfiguracija.getBytes());
		}
		catch (Exception e)
		{
			Error("error encrypting input configuration");
		}
		return BytesToHex(enkriptiranaKonfiguracija);
	}
	
	public static void ZapisiEnkriptiranuKonfiguraciju(String enkriptiranaKonfiguracija)
	{
		try
		{
			BufferedWriter pisac = new BufferedWriter(new FileWriter("config", false));
			pisac.write(enkriptiranaKonfiguracija);
			pisac.close();
		}
		catch(Exception e)
		{
			Error("error writing encrypted input configuration to file");
		}
	}
	
	public static void main(String[] args) throws IOException
	{
		String konfiguracija = UcitajKonfiguraciju();
		String enkriptiranaKonfiguracija = EnkriptirajKonfiguraciju(konfiguracija);
		ZapisiEnkriptiranuKonfiguraciju(enkriptiranaKonfiguracija);
		System.out.println("Oracle database agent encryptor successfully created config file");
	}
}
