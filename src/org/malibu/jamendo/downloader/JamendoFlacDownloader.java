package org.malibu.jamendo.downloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;

import javax.net.ssl.HttpsURLConnection;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.flac.FlacFileReader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JamendoFlacDownloader {
	
	public static final String JAMENDO_API_ALBUM_URL_STUB = "https://api.jamendo.com/v3.0/albums/tracks?client_id=9d9f42e3&format=json&order=track_position&id=%s&imagesize=600&audioformat=flac&audiodlformat=flac";
	
	public static void main(String[] args) throws IOException, KeyNotFoundException, CannotReadException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
		JamendoFlacDownloader downloader = new JamendoFlacDownloader();
		downloader.downloadAlbum("C:/Users/Ampp33/Desktop", "150083");
	}
	
	private JamendoProgressToken progressToken = null;
	
	public JamendoFlacDownloader() {}
	
	public JamendoFlacDownloader(JamendoProgressToken progressToken) {
		this.progressToken = progressToken;
	}
	
	public void downloadAlbum(String workingDirectoryPath, String albumId) throws IOException {
		String jamendoAlbumApiUrl = String.format(JAMENDO_API_ALBUM_URL_STUB, albumId);
		
		HttpsURLConnection conn = null;
		InputStream albumJsonStream = null;
		try {
			updateStatus("Connecting to Jamendo...");
			conn = (HttpsURLConnection)new URL(jamendoAlbumApiUrl).openConnection();
			albumJsonStream = conn.getInputStream();
			updateStatus("Connected");
			
			updateStatus("Parsing Album API response...");
			JSONTokener parser = new JSONTokener(albumJsonStream);
			JSONObject jamendoAlbumObj = new JSONObject(parser);
			JSONObject headers = jamendoAlbumObj.getJSONObject("headers");
			if("success".equals(headers.getString("status")) && 0 == headers.getInt("code")) {
				updateStatus("Successful response received...");
				// successful respose, do stuff!
			} else {
				updateStatus("Unsuccessful response, an error occurred");
				// something bad happened, stop
				return;
			}
			
			JSONArray albums = jamendoAlbumObj.getJSONArray("results");
			for(Object albumObj : albums) {
				JSONObject album = (JSONObject) albumObj;
				String artist = album.getString("artist_name");
				String albumName = album.getString("name");
				String albumYear = album.getString("releasedate").split("-")[0];
				String albumArtUrl = album.getString("image");
				
				updateStatus("Creating album directory...");
				File albumDir = createAlbumDir(workingDirectoryPath, artist, albumName);
				updateStatus("Album directory created");
				
				JSONArray tracks = album.getJSONArray("tracks");
				int noOfTracks = tracks.length();
				for (Object trackObj : tracks) {
					JSONObject track = (JSONObject) trackObj;
					int trackNo = Integer.parseInt(track.getString("position"));
					String trackName = track.getString("name");
					String trackDownloadUrl = track.getString("audiodownload");
					
					updateStatus("Downloading track " + trackNo + "/" + noOfTracks +": " + trackName);
					System.out.println(trackName + ": " + trackDownloadUrl);
					File trackFile = downloadTrack(trackDownloadUrl, albumDir.getAbsolutePath(), noOfTracks, trackNo, trackName);
					updateStatus("Updating track" + trackNo + "/" + noOfTracks + " metadata...");
//					updateTrackMetadata(artist, albumName, albumYear, albumArtUrl, noOfTracks, trackNo, trackName, trackFile);
					updateStatus("Track" + trackNo + "/" + noOfTracks + " completed!");
				}
			}
		} finally {
			if(conn != null) {
				conn.disconnect();
			}
			if(albumJsonStream != null) {
				albumJsonStream.close();
			}
		}
	}

	/**
	 * Updates metadata on the supplied track file
	 * 
	 * @param artist
	 * @param albumName
	 * @param albumYear
	 * @param albumArtUrl
	 * @param noOfTracks
	 * @param trackNo
	 * @param trackName
	 * @param trackFile
	 * @throws IOException
	 */
	private void updateTrackMetadata(String artist, String albumName, String albumYear, String albumArtUrl,
			int noOfTracks, int trackNo, String trackName, File trackFile) throws IOException {
		try {
			AudioFile audioFile = new FlacFileReader().read(trackFile);
			audioFile.setExt("flac");
			FlacTag flacTag = (FlacTag)audioFile.getTag();
			flacTag.setField(FieldKey.ARTIST, artist);
			flacTag.setField(FieldKey.ALBUM, albumName);
			flacTag.setField(FieldKey.YEAR, albumYear);
			flacTag.setField(FieldKey.TITLE, trackName);
			flacTag.setField(FieldKey.DISC_NO, "1");
			flacTag.setField(FieldKey.TRACK, Integer.toString(trackNo));
			flacTag.setField(FieldKey.TRACK_TOTAL, Integer.toString(noOfTracks));
			Artwork artwork = ArtworkFactory.getNew();
			artwork.setBinaryData(getAlbumArtworkAsByteArray(albumArtUrl));
			flacTag.addField(artwork);
			audioFile.commit();
		} catch (Exception ex) {
			throw new IOException(ex);
		}
	}
	
	/**
	 * Creates a directory to store all downloaded tracks
	 * 
	 * @param workingDirPath directory to crate album directory in
	 * @param artistName
	 * @param albumName
	 * @return File object specifying the location of the new album directory
	 * @throws IOException
	 */
	private File createAlbumDir(String workingDirPath, String artistName, String albumName) throws IOException {
		File workingDir = new File(workingDirPath);
		if(workingDirPath == null || !workingDir.exists() || !workingDir.isDirectory()) {
			return null;
		}
		if(artistName == null || artistName.trim().length() == 0) {
			artistName = "Artist";
		}
		if(albumName == null || albumName.trim().length() == 0) {
			artistName = "Album";
		}
		String albumAbsolutePath = workingDir.getAbsolutePath()
								+ File.separator
								+ artistName + " - " + albumName + " [FLAC]";
		File albumDir = new File(albumAbsolutePath);
		if(albumDir.exists()) {
			throw new IOException("destination album directory already exists: " + albumAbsolutePath);
		}
		albumDir.mkdirs();
		return albumDir;
	}
	
	private static final DecimalFormat df = new DecimalFormat("###.##");
	
	/**
	 * Downloads the supplied track
	 * 
	 * @param trackDownloadUrl URl of track
	 * @param albumDirPath album directory to download file to
	 * @param totalTrackNo total number of tracks in the album the track exists in
	 * @param trackNo track number
	 * @param trackName name of track
	 * @return File object specifying the location of the downloaded file
	 * @throws IOException
	 */
	private File downloadTrack(String trackDownloadUrl, String albumDirPath, int totalTrackNo, int trackNo, String trackName) throws IOException {
		if(trackName == null || trackName.trim().length() == 0) {
			trackName = "Track";
		}
		
		int numLeadingZeros = 0;
		int temp = totalTrackNo;
		while((temp /= 10) != 0) {
			numLeadingZeros++;
		}
		String zeroPaddingFormatString = "%d";
		if(numLeadingZeros > 0) {
			zeroPaddingFormatString = "%0" + numLeadingZeros + "d";
		}
		
		String trackAbsolutePath = albumDirPath + File.separator
									+ String.format(zeroPaddingFormatString, trackNo)
									+ " " + trackName + ".flac";
		
		File trackFile = new File(trackAbsolutePath);
		
		if(trackFile.exists()) {
			throw new IOException("destination track already exists: " + trackAbsolutePath);
		}
		
		// download file
		try(InputStream stream = new URL(trackDownloadUrl).openStream();
			FileOutputStream fos = new FileOutputStream(trackFile)) {
			byte[] buffer = new byte[5120000];
			int bytesRead = -1;
			double totalBytesRead = 0;
			while((bytesRead = stream.read(buffer, 0, buffer.length)) != -1) {
				totalBytesRead += bytesRead;
				updateStatus("Downloading track " + trackNo + "/" + totalTrackNo +" [ " + Double.valueOf(df.format(totalBytesRead / 1000000.0)) + "MB ]");
				fos.write(buffer, 0, bytesRead);
			}
			fos.flush();
		}
		
		return trackFile;
	}
	
	/**
	 * Returns the album art image specified by the supplied URL as a byte stream
	 * 
	 * @param albumArtUrl
	 * @return album art image as a byte stream
	 * @throws IOException
	 */
	private byte[] getAlbumArtworkAsByteArray(String albumArtUrl) throws IOException {
		// download file
		try(InputStream stream = new URL(albumArtUrl).openStream();
			ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[51200];
			int bytesRead = -1;
			while((bytesRead = stream.read(buffer, 0, buffer.length)) != -1) {
				bos.write(buffer, 0, bytesRead);
			}
			
			// will get executed after the "finally" where all streams are closed
			return bos.toByteArray();
		}
	}
	
	private void updateStatus(String status) {
		if(progressToken != null) {
			progressToken.setMessage(status);
		}
	}
	
}
