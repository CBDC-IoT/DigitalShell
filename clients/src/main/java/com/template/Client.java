package com.template;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.NetworkHostAndPort;
import DigitalShell.flows.DigitalShellTokenCreateAndIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.corda.core.utilities.NetworkHostAndPort.parse;

/**
 * Connects to a Corda node via RPC and performs RPC operations on the node.
 *
 * The RPC connection is configured using command line arguments.
 */
public class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) {
        // Create an RPC connection to the node.
        if (args.length != 3) throw new IllegalArgumentException("Usage: Client <node address> <rpc username> <rpc password>");
        final NetworkHostAndPort nodeAddress = parse(args[0]);
        final String rpcUsername = args[1];
        final String rpcPassword = args[2];
        final CordaRPCClient client = new CordaRPCClient(nodeAddress);
        final CordaRPCConnection clientConnection = client.start(rpcUsername, rpcPassword);
        final CordaRPCOps proxy = clientConnection.getProxy();

        // Interact with the node.
        // Example #1, here we print the nodes on the network.
        final List<NodeInfo> nodes = proxy.networkMapSnapshot();
        System.out.println("\n-- Here is the networkMap snapshot --");
        logger.info("{}", nodes);


        Party partyA = proxy.nodeInfo().getLegalIdentities().get(0);

        // Example #2, here we print the PartyA's node info
        CordaX500Name name = proxy.nodeInfo().getLegalIdentities().get(0).getName();//nodeInfo().legalIdentities.first().name
        System.out.println("\n-- Here is the node info of the node that the client connected to --");
        logger.info("{}", name);

        FlowHandle<SignedTransaction> signedTransactionFlowHandle = proxy.startFlowDynamic(DigitalShellTokenCreateAndIssue.CreateDigitalShellTokenFlow.class,  10000,  "CustomerA",  "ABC");
        logger.info("{}", signedTransactionFlowHandle);


        //Close the client connection
        clientConnection.close();

    }
}