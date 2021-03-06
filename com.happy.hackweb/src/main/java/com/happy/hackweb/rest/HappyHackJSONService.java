package com.happy.hackweb.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONObject;

import storm.starter.spout.InAppNotificationSpout;
import storm.starter.spout.MailNotifySpout;
import storm.starter.spout.MailingBolt;
import storm.starter.spout.NotificationBolt;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;

@Path("activity")
public class HappyHackJSONService {

	private static LocalCluster cluster = new LocalCluster();

	private static InAppNotificationSpout inAppNot = null;
	private static MailNotifySpout mailSpout = null;

	private static void setTopologies() {
		try {
			inAppNot = new InAppNotificationSpout();
			mailSpout = new MailNotifySpout();

			Config config = new Config();
			config.setDebug(false);
			//config.setNumWorkers(20);
			config.put(Config.TOPOLOGY_MAX_SPOUT_PENDING, 500);

			TopologyBuilder builder = new TopologyBuilder();
			builder.setSpout("inApp-spout", inAppNot);
			builder.setBolt("inApp-bolt", new NotificationBolt())
					.shuffleGrouping("inApp-spout");

			TopologyBuilder mailBuilder = new TopologyBuilder();
			mailBuilder.setSpout("mail-spout", mailSpout);
			mailBuilder.setBolt("mail-bolt", new MailingBolt())
					.shuffleGrouping("mail-spout");

			cluster.submitTopology("InAppTopology", config,
					builder.createTopology());
			cluster.submitTopology("MailTopology", config,
					mailBuilder.createTopology());

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createXmlEventsInJSON(String request) {

		if (null == inAppNot) {
			setTopologies();
		}
		//System.out.println("request is===" + request);
		if(request.replaceAll(" ", "").replaceAll("\n", "").contains(":}")){
			
			request =request.replace("\"price\" :", "\"price\" : -1");
			System.out.println(request);
		}
		try {
			
			JSONObject jsonObject = new JSONObject(request);
				
			inAppNot.queue.put(jsonObject);
			mailSpout.queue.put(new JSONObject(request));
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return Response.status(200).entity("").build();

	}

	@GET
	@Path("shutdown")
	public Response shutDown() {

		System.out.println("Stopping the cluster");
		cluster.shutdown();
		return Response.status(200).entity("").build();
	}

}
