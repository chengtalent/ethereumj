package com.ethercamp.starter.ethereum;

import org.ethereum.core.*;
import org.ethereum.crypto.ECKey;
import org.ethereum.facade.Blockchain;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.EthereumFactory;
import org.ethereum.facade.Repository;
import org.ethereum.jsonrpc.TypeConverter;
import org.ethereum.mine.BlockMiner;
import org.ethereum.mine.Ethash;
import org.ethereum.mine.MinerListener;
import org.ethereum.solidity.compiler.CompilationResult;
import org.ethereum.util.ByteUtil;
import org.ethereum.vm.program.ProgramResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class EthereumBean {

    public Ethereum ethereum;
    public BlockMiner blockMiner;

    private static final Logger logger = LoggerFactory.getLogger("mine");

    public void start(){
        this.ethereum = EthereumFactory.createEthereum();
        this.ethereum.addListener(new EthereumListener(ethereum));
    }

    public String getBestBlock(){
        return "" + ethereum.getBlockchain().getBestBlock().getNumber() + "\n"
                + ethereum.getBlockchain().getBestBlock().toString();
    }

    public String getBlockByNumber(int index){
        return ethereum.getBlockchain().getBlockByNumber(index).toString();
    }

    public String getBlockByHash(byte[] hash){
        return ethereum.getBlockchain().getBlockByHash(hash).toString();
    }

    public String getTransactionInfo(byte[] hash){
        TransactionInfo transactionInfo = ((org.ethereum.core.Blockchain)(ethereum.getBlockchain())).getTransactionInfo(hash);
        return transactionInfo.getReceipt().getTransaction().toString() + "\n"
                + transactionInfo.getReceipt().toString();
    }

    public List<Transaction> getTransactionHistory(byte[] receive){
        List<Transaction> transactionList = new LinkedList<>();
        Blockchain blockchain = ethereum.getBlockchain();

        for(long i=blockchain.getBestBlock().getNumber(); i>=0; --i){
            Block block = blockchain.getBlockByNumber(i);
            if(block == null){
                logger.info("GetTransactionHistory - getBlockByNumber() return null for number: {}", i);
                continue;
            }

            List<Transaction> blockTxList = block.getTransactionsList();

            for(int j=blockTxList.size()-1; j>=0; --j){
                Transaction tx = blockTxList.get(j);
                if(Arrays.equals(tx.getReceiveAddress(), receive)){
                    transactionList.add(tx);
                }
            }
        }

        return transactionList;
    }

    public List<Transaction> getTransactionHistoryTrack(byte[] receive){
        List<Transaction> transactionList = new LinkedList<>();
        Blockchain blockchain = ethereum.getBlockchain();

        for(long i=blockchain.getBestBlock().getNumber(); i>=0; --i){
            Block block = blockchain.getBlockByNumber(i);
            if(block == null){
                logger.info("GetTransactionHistory - getBlockByNumber() return null for number: {}", i);
                continue;
            }

            List<Transaction> blockTxList = block.getTransactionsList();

            for(int j=blockTxList.size()-1; j>=0; --j){
                Transaction tx = blockTxList.get(j);
                if(Arrays.equals(tx.getReceiveAddress(), receive)){
                    transactionList.add(tx);
                    receive = tx.getSender();
                }
            }
        }

        return transactionList;
    }

    public String submitTransaction(String sendPK,
                                    String receive,
                                    String value,
                                    String data){
        ECKey senderKey = ECKey.fromPrivate(Hex.decode(sendPK));
        byte[] receiverAddr = Hex.decode(receive);
        byte[] address = senderKey.getAddress();

        Repository repository = ethereum.getRepository();

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
        Future<Transaction> ft = ethereum.submitTransaction(tx);

        try {
            return ft.get().toString();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return "error";
    }

    public String submitContract(String sendPK, String contractName, String func){
        CompilationResult cres = Utils.getContractCompilationResult(contractName);
        if(cres == null)
            return "";

        BlockchainImpl blockchain = (BlockchainImpl)ethereum.getBlockchain();
        ECKey sender = ECKey.fromPrivate(Hex.decode(sendPK));
        String contractAddressStr = null;

        if(cres.contracts.get(contractName) != null) {
            Transaction tx = createTx(blockchain, sender, new byte[0], Hex.decode(cres.contracts.get(contractName).bin), 0L);
            executeTransaction(blockchain, tx);

            byte[] contractAddress = tx.getContractAddress();
            contractAddressStr = Hex.toHexString(contractAddress);

            CallTransaction.Contract contract1 = new CallTransaction.Contract(cres.contracts.get(contractName).abi);
            byte[] callData = contract1.getByName(func).encode();

            Transaction tx1 = createTx(blockchain, sender, contractAddress, callData);
            ProgramResult programResult = executeTransaction(blockchain, tx1);

            // suicide of a single account should be counted only once  for contract PsychoKiller.sol
            // Assert.assertEquals(programResult.getFutureRefund(), 24000);
            return TypeConverter.toJsonHex(programResult.getHReturn()) + "\n"
                    + programResult.getHReturn() + "\n"
                    + contractAddressStr + "\n"
                    + String.valueOf(programResult.getFutureRefund());
        }

        return "";
    }

    private Transaction createTx(BlockchainImpl blockchain, ECKey sender, byte[] receiveAddress, byte[] data) {
        return createTx(blockchain, sender, receiveAddress, data, 0);
    }

    private Transaction createTx(BlockchainImpl blockchain, ECKey sender, byte[] receiveAddress, byte[] data, long value) {
        BigInteger nonce = blockchain.getRepository().getNonce(sender.getAddress());
        Transaction tx = new Transaction(
                ByteUtil.bigIntegerToBytes(nonce),
                ByteUtil.longToBytesNoLeadZeroes(0L),
                ByteUtil.longToBytesNoLeadZeroes(0xfffff),
                receiveAddress,
                ByteUtil.longToBytesNoLeadZeroes(value),
                data);
        tx.sign(sender.getPrivKeyBytes());
        return tx;
    }

    private ProgramResult executeTransaction(BlockchainImpl blockchain, Transaction tx) {
        org.ethereum.core.Repository track = blockchain.getRepository().startTracking();
        TransactionExecutor executor = new TransactionExecutor(tx, new byte[32], blockchain.getRepository(),
                blockchain.getBlockStore(), blockchain.getProgramInvokeFactory(), blockchain.getBestBlock());

        executor.init();
        executor.execute();
        executor.go();
        executor.finalization();

        track.commit();
        return executor.getResult();
    }

    public BlockMiner getBlockMiner(){
        return ethereum.getBlockMiner();
    }

    public void startMining(){
        if(blockMiner == null) {

            blockMiner = ethereum.getBlockMiner();
            blockMiner.addListener(new MinerListener() {
                @Override
                public void miningStarted() {
                    System.out.println("=== Miner.miningStarted");
                }

                @Override
                public void miningStopped() {
                    System.out.println("=== Miner.miningStopped");
                }

                @Override
                public void blockMiningStarted(Block block) {
                    System.out.println("=== Miner.blockMiningStarted " + blockInfo(block));
                }

                @Override
                public void blockMined(Block block) {
//                boolean validate = Ethash.getForBlock(block.getNumber()).validate(block.getHeader());
                    System.out.println("=== Miner.blockMined " + blockInfo(block));
//                System.out.println("=== MinerTest.blockMined: " + validate);
                }

                @Override
                public void blockMiningCanceled(Block block) {
                    System.out.println("=== Miner.blockMiningCanceled " + blockInfo(block));
                }
            });

        }

        Ethash.fileCacheEnabled = true;
        blockMiner.setFullMining(true);
        blockMiner.startMining();
    }

    public void stopMining() {
        if(blockMiner != null){
            blockMiner.stopMining();
        }
    }

    static String blockInfo(Block b) {
        boolean ours = Hex.toHexString(b.getExtraData()).startsWith("cccccccccc");
        String txs = "Tx[";
        for (Transaction tx : b.getTransactionsList()) {
            txs += ByteUtil.byteArrayToLong(tx.getNonce()) + ", ";
        }
        txs = txs.substring(0, txs.length() - 2) + "]";
        return (ours ? "##" : "  ") + b.getShortDescr() + " " + txs;
    }

}
