package com.ethercamp.starter.ethereum;

import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by chengsilei on 16/5/18.
 */
public class Contract {

//    public static final String contractPsychoKiller =
//            "contract PsychoKiller {" +
//                    "    function homicide() {" +
//                    "        suicide(msg.sender);" +
//                    "    }" +
//                    "    function multipleHomocide() {" +
//                    "        PsychoKiller k  = this;" +
//                    "        k.homicide();" +
//                    "        k.homicide();" +
//                    "        k.homicide();" +
//                    "        k.homicide();" +
//                    "    }" +
//                    "}";

    public String getSolidityCode(String contractName) {
        if(StringUtils.isEmpty(contractName))
            return "";

        InputStream is = getClass().getResourceAsStream("/sol/" + contractName + ".sol");
        if(is == null)
            return "";

        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

        return result.toString();
    }

//    public static void main(String[] args){
//        System.out.println(new Contract().getSolidityCode());
//    }
}
