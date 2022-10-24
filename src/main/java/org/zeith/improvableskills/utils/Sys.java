package org.zeith.improvableskills.utils;

import net.minecraft.Util;
import org.zeith.hammerlib.util.java.Threading;

public class Sys
{
	public static void openURL(String url)
	{
		Threading.createAndStart("OpenURL", () ->
		{
			try
			{
				Util.getPlatform().openUri(url);
			} catch(Throwable e)
			{
				e.printStackTrace();
			}
		});
	}
}