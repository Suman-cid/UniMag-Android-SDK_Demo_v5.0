package com.idtechproducts.acom;

import android.media.AudioTrack;

import idtech.msr.xmlmanager.ConfigParameters;

public abstract interface AcomManagerMsg
{
	public abstract void onAttachmentChange(boolean paramBoolean);

	public abstract void onAutoConfigProgress(int paramInt);

	public abstract void onAutoConfigStopped(ConfigParameters paramConfigParameters);

	public abstract void onVolumeAdjustFailure(int paramInt, String paramString);

	public static abstract interface CommandEncoder
	{
		public abstract AudioTrack getCommandAudioTrack(byte[] paramArrayOfByte, int paramInt1, int paramInt2, int paramInt3);
	}
}