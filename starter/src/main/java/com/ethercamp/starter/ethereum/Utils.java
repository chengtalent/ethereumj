package com.ethercamp.starter.ethereum;

import org.ethereum.core.Repository;
import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.spongycastle.util.encoders.Hex;

import java.util.Iterator;
import java.util.List;

/**
 * Created by chengsilei on 16/4/30.
 */
public class Utils {

    public static Repository getImplRepository(EthereumBean ethereumBean){
        org.ethereum.facade.Repository repository = ethereumBean.ethereum.getRepository();
        org.ethereum.core.Repository coreRep = (org.ethereum.core.Repository) repository;

        return coreRep;
    }

    public static org.ethereum.db.RepositoryTrack getTrackRepository(EthereumBean ethereumBean){
        org.ethereum.facade.Repository repository = ethereumBean.ethereum.getRepository();
        org.ethereum.db.RepositoryTrack trackRep = (org.ethereum.db.RepositoryTrack) repository;

        return trackRep;
    }

    public static byte[] getAddress(String privateKey){
        ECKey senderKey = ECKey.fromPrivate(Hex.decode(privateKey));
        byte[] address = senderKey.getAddress();

        return address;
    }

    public static String convertToString(List<Transaction> transactionList){
        String str = "";
        str += (transactionList.size() + "\n");

        for (Iterator<Transaction> iter = transactionList.iterator(); iter.hasNext(); ){
            Transaction tx = iter.next();
            str += (tx.toString() + "\n");
        }

        return str;
    }
}
