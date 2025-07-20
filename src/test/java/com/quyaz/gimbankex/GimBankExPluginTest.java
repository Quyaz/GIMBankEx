package com.quyaz.gimbankex;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GimBankExPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GimBankExPlugin.class);
		RuneLite.main(args);
	}
}