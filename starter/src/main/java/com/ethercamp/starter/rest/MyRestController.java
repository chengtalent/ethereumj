package com.ethercamp.starter.rest;


import com.ethercamp.starter.ethereum.EthereumBean;
import com.ethercamp.starter.ethereum.Utils;
import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.facade.Repository;
import org.ethereum.net.rlpx.Node;
import org.ethereum.util.ByteUtil;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.Future;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class MyRestController {

    @Autowired
    EthereumBean ethereumBean;

    @RequestMapping(value = "/bestBlock", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getBestBlock() throws IOException {
        return ethereumBean.getBestBlock();
    }

    @RequestMapping(value = "/getBlock", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getBlock(@RequestParam("index") int index) throws IOException {
        return ethereumBean.getBlockByNumber(index);
    }

    @RequestMapping(value = "/getAddress", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getAddress(@RequestParam("pk") String pk) throws IOException {
        return Hex.toHexString(Utils.getAddress(pk));
    }

    @RequestMapping(value = "/createAccount", method = POST, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String createAccount(@RequestParam("pk") String pk) throws IOException {
        org.ethereum.core.Repository coreRep = Utils.getImplRepository(ethereumBean);

        String r = coreRep.createAccount(Utils.getAddress(pk)).toString();
        r += ";\n";
        r += Hex.toHexString(Utils.getAddress(pk));

        coreRep.flush();

        return r;
    }

    @RequestMapping(value = "/getAccount", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getAllAccount(@RequestParam("address") String address) throws IOException {
        org.ethereum.core.Repository coreRep = Utils.getImplRepository(ethereumBean);

        return coreRep.getAccountState(Hex.decode(address)).toString();
    }

    @RequestMapping(value = "/addAccount", method = POST, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String addAccount(@RequestParam("address") String address) throws IOException {
        org.ethereum.core.Repository coreRep = Utils.getImplRepository(ethereumBean);

        return coreRep.addBalance(Hex.decode(address), BigInteger.valueOf(100)).toString();
    }

    @RequestMapping(value = "/getPeers", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getPeers() throws IOException {
        return ethereumBean.ethereum.getPeers().toString();
    }

    @RequestMapping(value = "/connectPeer", method = POST, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String connectPeer(@RequestParam("node") String node) throws IOException {
        Node n = new Node(node);

        ethereumBean.ethereum.connect(n);
        return "Success";
    }

    @RequestMapping(value = "/accountBalance", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getAccountBalance(@RequestParam("address") String address) throws IOException {
        Repository repository = ethereumBean.ethereum.getRepository();

        return repository.getBalance(Hex.decode(address)).toString();
    }

    @RequestMapping(value = "/transaction", method = POST, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String submiteTransaction(@RequestParam("sendPK") String sendPK,
                                     @RequestParam("receive") String receive,
                                     @RequestParam("value") String value,
                                     @RequestParam("data") String data) throws IOException {

        ECKey senderKey = ECKey.fromPrivate(Hex.decode(sendPK));
        byte[] receiverAddr = Hex.decode(receive);
        byte[] address = senderKey.getAddress();

        Repository repository = ethereumBean.ethereum.getRepository();

        int i = repository.getNonce(address).intValue();
        long lValue = Long.parseLong(value);

        Transaction tx = new Transaction(
                ByteUtil.intToBytesNoLeadZeroes(i),
                ByteUtil.longToBytesNoLeadZeroes(0L),
                //ByteUtil.longToBytesNoLeadZeroes(ethereumBean.ethereum.getGasPrice()),
                ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                receiverAddr,
                ByteUtil.longToBytesNoLeadZeroes(lValue),
                data.getBytes());

        tx.sign(senderKey.getPrivKeyBytes());
        System.out.println("=== Submitting tx: " + tx);
        Future<Transaction> ft = ethereumBean.ethereum.submitTransaction(tx);

        return ft.toString();
    }

    @RequestMapping(value = "/close", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String close() throws IOException {
        org.ethereum.core.Repository coreRep = Utils.getImplRepository(ethereumBean);

        coreRep.close();
        return "Success";
    }

    @RequestMapping(value = "/startMining", method = POST, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String startMining() throws IOException {

        ethereumBean.startMining();
        return "Success";
    }

    @RequestMapping(value = "/stopMining", method = POST, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String stopMining() throws IOException {

        ethereumBean.stopMining();
        return "Success";
    }

    @RequestMapping(value = "/getTransaction", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getTransaction(@RequestParam("hash") String hash) throws IOException {
        byte[] hashCode = Hex.decode(hash);
        return ethereumBean.getTransactionInfo(hashCode);
    }

    @RequestMapping(value = "/getBlockByHash", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getBlockByHash(@RequestParam("hash") String hash) throws IOException {
        byte[] hashCode = Hex.decode(hash);
        return ethereumBean.getBlockByHash(hashCode);
    }

    @RequestMapping(value = "/getTransactionHistory", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getTransactionHistory(@RequestParam("receive") String receive) throws IOException {
        byte[] receiverAddr = Hex.decode(receive);
        return Utils.convertToString(ethereumBean.getTransactionHistory(receiverAddr));
    }

    @RequestMapping(value = "/getTransactionHistoryTrack", method = GET, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getTransactionHistoryTrack(@RequestParam("receive") String receive) throws IOException {
        byte[] receiverAddr = Hex.decode(receive);
        return Utils.convertToString(ethereumBean.getTransactionHistoryTrack(receiverAddr));
    }
}
