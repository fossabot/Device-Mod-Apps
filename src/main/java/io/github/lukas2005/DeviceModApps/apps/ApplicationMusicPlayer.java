package io.github.lukas2005.DeviceModApps.apps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.mrcrayfish.device.api.app.Component;
import com.mrcrayfish.device.api.app.Layout;
import com.mrcrayfish.device.api.app.component.Button;
import com.mrcrayfish.device.api.app.component.ItemList;
import com.mrcrayfish.device.api.app.component.ProgressBar;
import com.mrcrayfish.device.api.app.listener.ClickListener;

import io.github.lukas2005.DeviceModApps.objects.ListedSong;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import javazoom.spi.vorbis.sampled.file.VorbisAudioFileReader;
import net.minecraft.nbt.NBTTagCompound;

public class ApplicationMusicPlayer extends ApplicationBase {
	
	/**
	 * Will be used when sound cloud will allow for registration of apps again - currently unused
	 */
	//ApiWrapper api = new ApiWrapper("", "", null, null);
	
	ItemList<ListedSong> playList;
	
	public static ArrayList<ListedSong> defaultRecords = new ArrayList<>();
	
	boolean isPlaying = false;
	
	public SoundPlayingThread soundThread;
	
	@Override
	public void init() {
		Layout main = new Layout();
		setCurrentLayout(main);
		
		playList = new ItemList<>(5, 5, 75, 6);
		
		//playList.addItem(new ListedSong("C418 - Cat", new File("")));
		//markDirty();
		
		
		for (ListedSong e : defaultRecords) {
			if (!playList.getItems().contains(e)) {
				playList.addItem(e);
				markDirty();
			}
		}
		
		main.addComponent(playList);
		
		final Button play = new Button("Play", 100, 10, 20, 20);
		
		main.addComponent(play);
		
		final Button pause = new Button("Pause", 130, 10, 20, 20);
		pause.setEnabled(false);
		main.addComponent(pause);
		
		final Button stop = new Button("Stop", 160, 10, 20, 20);
		stop.setEnabled(false);
		main.addComponent(stop);
		
		final ProgressBar progress = new ProgressBar(100, 50, 80, 10);
		main.addComponent(progress);
		
		play.setClickListener(new ClickListener() {
			@Override
			public void onClick(Component c, int mouseButton) {
				if (playList.getSelectedItem() != null) {
					if (soundThread == null) { 
						soundThread = new SoundPlayingThread(playList.getSelectedItem().file, progress);
						soundThread.addEndedListener(new Runnable() {
							@Override
							public void run() {
								if (soundThread != null) {
									soundThread.close();
									soundThread = null;
									isPlaying = false;
									pause.setEnabled(isPlaying);
									stop.setEnabled(false);
									play.setEnabled(!isPlaying);
								}
							}
						});
						soundThread.start();
					} else if (soundThread != null) {
						soundThread.play();
					}
					isPlaying = true;
					pause.setEnabled(isPlaying);
					stop.setEnabled(true);
					play.setEnabled(!isPlaying);
				}
			}
		});
		
		pause.setClickListener(new ClickListener() {
			@Override
			public void onClick(Component c, int mouseButton) {
				if (soundThread != null) {
					soundThread.pause();
					isPlaying = false;
					pause.setEnabled(isPlaying);
					stop.setEnabled(true);
					play.setEnabled(!isPlaying);
				}
			}
		});
		
		stop.setClickListener(new ClickListener() {
			@Override
			public void onClick(Component c, int mouseButton) {
				if (soundThread != null) {
					soundThread.close();
					soundThread = null;
					isPlaying = false;
					pause.setEnabled(isPlaying);
					stop.setEnabled(false);
					play.setEnabled(!isPlaying);
				}
			}
		});
		
	}
	
	@Override
	public void load(NBTTagCompound nbt) {
		NBTTagCompound songList = nbt.getCompoundTag("songList");
		playList.removeAll();
		for (String key : songList.getKeySet()) {
			playList.addItem(new ListedSong(key, new File(songList.getString(key))));
		}
	}

	@Override
	public void save(NBTTagCompound nbt) {
		NBTTagCompound songList = new NBTTagCompound();
		for (ListedSong s : playList.getItems()) {
			songList.setString(s.name, s.file.getAbsolutePath());
		}
		nbt.setTag("songList", songList);
	}
	
	@Override
	public void onClose() {
		if (soundThread != null) {
			soundThread.close();
			soundThread = null;
		}
	}
	
	public class SoundPlayingThread extends Thread {
		
		File audioFile;
		Clip clip;
		
		public long time = 0;
		
		ProgressBar progress;
		Thread progressUpdateThread;
		
		
		private ArrayList<Runnable> listeners = new ArrayList<>();
		
		public SoundPlayingThread(File audioFile, ProgressBar progress) {
			this.audioFile = audioFile;
			this.progress = progress;
		}
		
		public SoundPlayingThread(File audioFile) {
			this(audioFile, null);
		}
		
		public void pause() {
			time = clip.getMicrosecondPosition();
			clip.stop();
		}
		
		public void play() {
			clip.start();
			clip.setMicrosecondPosition(time);
			if (progress != null) {
				progress.setMax((int) clip.getMicrosecondLength());
				
				progressUpdateThread = new Thread("Progressbar Update Thread") {
					@Override
					public void run() {
						while (!Thread.interrupted()) {
							progress.setProgress((int) clip.getMicrosecondPosition()); // /1000000
						}
					}
				};
				
				progressUpdateThread.start();
			}
		}
		
		public void close() {
			clip.stop();
			clip.close();
			if (progressUpdateThread != null) progressUpdateThread.interrupt();
			Thread.currentThread().interrupt();
		}
		
		 AudioInputStream createOggMp3(File fileIn) throws IOException, Exception {
			    AudioInputStream audioInputStream=null;
			    AudioFormat targetFormat=null;
			    try {
			      AudioInputStream in=null;
			      if(fileIn.getName().endsWith(".ogg")) {
			        VorbisAudioFileReader vb=new VorbisAudioFileReader();
			        in=vb.getAudioInputStream(fileIn);
			      }
			      else if(fileIn.getName().endsWith(".mp3")) {
			        MpegAudioFileReader mp=new MpegAudioFileReader();
			        in=mp.getAudioInputStream(fileIn);
			      }
			      AudioFormat baseFormat=in.getFormat();
			      targetFormat=new AudioFormat(
			              AudioFormat.Encoding.PCM_SIGNED,
			              baseFormat.getSampleRate(),
			              16,
			              baseFormat.getChannels(),
			              baseFormat.getChannels() * 2,
			              baseFormat.getSampleRate(),
			              false);
			      audioInputStream=AudioSystem.getAudioInputStream(targetFormat, in);
			    }
			    catch(UnsupportedAudioFileException ue) { System.out.println("\nUnsupported Audio"); }
			    return audioInputStream;
			  }
		
		public void addEndedListener(Runnable run) {
			listeners.add(run);
		}
		 
		@Override
		public void run() {
	        try {
                AudioInputStream audioInputStream;
				if(audioFile.getName().endsWith(".ogg") || audioFile.getName().endsWith(".mp3")) {
					audioInputStream = createOggMp3(audioFile);
				}
				else { // wav
					audioInputStream = AudioSystem.getAudioInputStream(audioFile);
				}
                clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                play();
                Thread.sleep((clip.getMicrosecondLength()-time)/1000);
                for (Runnable run : listeners) {
                	new Thread(run).start();
                }
                close();
            } catch(InterruptedException ex) {
            	Thread.currentThread().interrupt();
            } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            	System.out.println("Error playing sound");
            	e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}
