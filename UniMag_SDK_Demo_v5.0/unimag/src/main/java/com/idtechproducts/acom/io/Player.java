package com.idtechproducts.acom.io;

import android.media.AudioTrack;

import com.idtechproducts.acom.AcomManagerMsg;

import idtech.msr.xmlmanager.ConfigParameters;

class Player {
	private final AcomManagerMsg.CommandEncoder _commandEncoder;
	private ConfigParameters _config;
	private AudioTrack _command_At;

	public Player(AcomManagerMsg.CommandEncoder commandEncoder) {
		_commandEncoder = commandEncoder;
	}

	public void release() {
		stopCommand();
	}

	public void setConfig(ConfigParameters newConfig) {
		if ((_command_At != null) && (_command_At.getPlayState() == 3))
			throw new IllegalStateException();
		if (newConfig == null) {
			throw new NullPointerException();
		}

		release();

		_config = newConfig.clone();
	}

	public void startPlayingCommand(byte[] commandString) {
		if (commandString == null)
			throw new NullPointerException();
		if (_config == null) {
			throw new IllegalStateException("config not loaded");
		}

		stopCommand();

		_command_At = _commandEncoder.getCommandAudioTrack(
				commandString, _config.getFrequenceOutput(), _config.getDirectionOutputWave(), _config.getPowerupLastBeforeCMD());
		_command_At.play();
	}

	public void stopCommand() {
		if (_command_At != null) {
			_command_At.stop();
			_command_At.release();
			_command_At = null;
		}
	}
}