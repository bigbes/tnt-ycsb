package com.yahoo.ycsb.db; 

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import tarantool.connector.*;
import tarantool.connector.request.*;
import tarantool.connector.exception.TarantoolConnectorException;
import tarantool.common.Tuple;

import com.yahoo.ycsb.ByteArrayByteIterator;
import com.yahoo.ycsb.ByteIterator;
import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.DBException;
import com.yahoo.ycsb.StringByteIterator;

public class TarantoolClient extends DB{
	Connection conn;
	Properties props = getProperties();
	private static Logger logger = Logger.getLogger("com.yahoo.ycsb.db.tarantoolclient");
	
	public static final String HOST_PROPERTY = "tnt.host";
    public static final String PORT_PROPERTY = "tnt.port";

	public void init() throws DBException{
		conn = new ConnectionImpl();
		
        String address = props.getProperty(HOST_PROPERTY, "127.0.0.1");
        int port = Integer.parseInt(props.getProperty(PORT_PROPERTY, "33013"));
        
		try {
			conn.connect(address,  port);
		} catch (TarantoolConnectorException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	  
	public void cleanup() throws DBException{
		conn.disconnect();
	}
	  
	@Override
	public int insert(String table, String key,
			HashMap<String, ByteIterator> values) {
		Tuple t = new Tuple();
		
		try {
			t.add(key.getBytes("UTF-8"));
			for (Map.Entry<String, ByteIterator> i: values.entrySet()){
				t.add(i.getKey().getBytes("UTF-8"));
				t.add(i.getValue().toArray());
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		Response resp = null;
		try {
			resp = conn.execute(new Insert(0, 0, t.toByte()));
		} catch (TarantoolConnectorException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (resp.get_error() != 0){
			//System.out.println(resp.get_error());
			logger.log(Level.WARNING, "INSERT "+resp.get_error());
			return 1;
		}
		
		return 0;
	}
	
	byte[] toByte(int value) { 
		return new byte[]{ 
				(byte)(value >>> 24), 
				(byte)(value >> 16 & 0xff), 
				(byte)(value >> 8 & 0xff), 
				(byte)(value & 0xff) 
				}; 
	}
	
	@Override
	public int read(String table, String key, Set<String> fields,
			HashMap<String, ByteIterator> result){
		Tuple tup = new Tuple();
		try {
			tup.add(key.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		Response resp = null;
		try {
			resp = conn.execute(new Select(0, 0, 0, 1, tup.toByte()));
		} catch (TarantoolConnectorException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (resp.get_error() != 0){
			logger.log(Level.WARNING, "READ "+resp.get_error());
			return 1;
		}
		
		if (resp.get_tuple().length == 0)
			return 1;
		
		byte[][] tuple = resp.get_tuple()[0];
		for (int i = 0; i < (tuple.length - 1) / 2; i++){
			String s = null;
			try {
				s = new String(tuple[2*i + 1], "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			if (fields == null || fields.contains(s))
				result.put(s, new ByteArrayByteIterator(tuple[2*i + 2]));
		}

		return 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public int scan(String table, String startkey, int recordcount, Set<String> fields,
			Vector<HashMap<String, ByteIterator>> result) {
		Tuple tup = new Tuple();
		tup.add("0".getBytes());
		tup.add("0".getBytes());
		tup.add(Integer.toString(recordcount).getBytes());
		
		try {
			tup.add(startkey.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		Response resp = null;
		
		try {
			resp = conn.execute(new Call(0, "box.select_range".getBytes(), tup.toByte()));
		} catch (TarantoolConnectorException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (resp.get_error() != 0){
			logger.log(Level.WARNING, "SCAN "+resp.get_error());
			return 1;
		}
		
		byte[][][] tuple = resp.get_tuple();
		HashMap<String, ByteIterator> temp = new HashMap<String, ByteIterator>();
		for (int i = 0; i < tuple.length; ++i){
			for (int j = 0; j < (tuple[i].length - 1) / 2; j++){
				
				String s = null;
				try {
					s = new String(tuple[i][2*j + 1], "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				if (fields == null || fields.contains(s)){
					try {
						temp.put(s, new StringByteIterator(new String(tuple[i][2*j+2], "UTF-8")));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
			result.add((HashMap<String, ByteIterator> )temp.clone());
			temp.clear();
		}
		
		return 0;
	}

	@Override
	public int delete(String table, String key) {
		Tuple tup = new Tuple();
		try {
			tup.add(key.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		Response resp = null;
		
		try {
			resp = conn.execute(new Delete(0, 0, tup.toByte()));
		} catch (TarantoolConnectorException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (resp.get_error() != 0){
			logger.log(Level.WARNING, "DELETE "+resp.get_error());
			return 1;
		}

		return 0;
	}

	@Override
	public int update(String table, String key,
			HashMap<String, ByteIterator> values) {
		return insert(table, key, values);
	}	
}