/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartloli.kafka.eagle.ipc;

import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartloli.kafka.eagle.ipc.KafkaOffsetServer;
import org.smartloli.kafka.eagle.util.SystemConfigUtils;

/**
 * Rpc through the Kafka stored in the offset.
 * 
 * @author smartloli.
 *
 *         Created by Jan 5, 2017
 */
public class TestRpcClient {

	private final static Logger LOG = LoggerFactory.getLogger(TestRpcClient.class);
	private final static int PORT = SystemConfigUtils.getIntProperty("kafka.eagle.offset.rpc.port");
	private final static String ADDR = "127.0.0.1";

	public static void main(String[] args) {
		System.out.println(getConsumer("cluster1"));
	}

	/** Get offset from kafka topic. */
	public static String getOffset(String clusterAlias,String bootstrapServers) {
		TTransport transport = new TFramedTransport(new TSocket(ADDR, PORT, 30000));
		TProtocol protocol = new TCompactProtocol(transport);
		KafkaOffsetServer.Client client = new KafkaOffsetServer.Client(protocol);
		String ret = "";
		try {
			transport.open();
			ret = client.getOffset(clusterAlias,bootstrapServers);
		} catch (Exception e) {
			LOG.error("Rpc Client getOffset has error,msg is " + e.getMessage());
		} finally {
			transport.close();
		}
		return ret;
	}

	/** Get activer consumer from kafka topic. */
	public static String getActiverConsumer(String clusterAlias) {
		TTransport transport = new TFramedTransport(new TSocket(ADDR, PORT, 30000));
		TProtocol protocol = new TCompactProtocol(transport);
		KafkaOffsetServer.Client client = new KafkaOffsetServer.Client(protocol);
		String ret = "";
		try {
			transport.open();
			ret = client.getActiverConsumer(clusterAlias);
		} catch (Exception e) {
			LOG.error("Rpc Client getOffset has error,msg is " + e.getMessage());
		} finally {
			transport.close();
		}
		return ret;
	}

	/** Get consumer information from kafka topic. */
	public static String getConsumer(String clusterAlias) {
		TTransport transport = new TFramedTransport(new TSocket(ADDR, PORT, 30000));
		TProtocol protocol = new TCompactProtocol(transport);
		KafkaOffsetServer.Client client = new KafkaOffsetServer.Client(protocol);
		String ret = "";
		try {
			transport.open();
			ret = client.getConsumer(clusterAlias);
		} catch (Exception e) {
			LOG.error("Rpc Client getOffset has error,msg is " + e.getMessage());
		} finally {
			transport.close();
		}
		return ret;
	}

}
