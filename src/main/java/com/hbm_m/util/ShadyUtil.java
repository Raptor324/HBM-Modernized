package com.hbm_m.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * 1:1 {@code com.hbm.util.ShadyUtil}: UUIDs fuer Accessoires/Sonderverhalten, die Schild-Hashes
 * (Bobmazon-Geheimkatalog) und {@link #smoosh}.
 */
public final class ShadyUtil {

    private ShadyUtil() {}

    public static final String HbMinecraft = "192af5d7-ed0f-48d8-bd89-9d41af8524f8";
    public static final String LPkukin = "937c9804-e11f-4ad2-a5b1-42e62ac73077";
    public static final String Dafnik = "3af1c262-61c0-4b12-a4cb-424cc3a9c8c0";
    public static final String a20 = "4729b498-a81c-42fd-8acd-20d6d9f759e0";
    public static final String LordVertice = "a41df45e-13d8-4677-9398-090d3882b74f";
    public static final String CodeRed_ = "912ec334-e920-4dd7-8338-4d9b2d42e0a1";
    public static final String dxmaster769 = "62c168b2-d11d-4dbf-9168-c6cea3dcb20e";
    public static final String Dr_Nostalgia = "e82684a7-30f1-44d2-ab37-41b342be1bbd";
    public static final String Samino2 = "87c3960a-4332-46a0-a929-ef2a488d1cda";
    public static final String Hoboy03new = "d7f29d9c-5103-4f6f-88e1-2632ff95973f";
    public static final String Dragon59MC = "dc23a304-0f84-4e2d-b47d-84c8d3bfbcdb";
    public static final String Steelcourage = "ac49720b-4a9a-4459-a26f-bee92160287a";
    public static final String ZippySqrl = "03c20435-a229-489a-a1a1-671b803f7017";
    public static final String Schrabby = "3a4a1944-5154-4e67-b80a-b6561e8630b7";
    public static final String SweatySwiggs = "5544aa30-b305-4362-b2c1-67349bb499d5";
    public static final String Drillgon = "41ebd03f-7a12-42f3-b037-0caa4d6f235b";
    public static final String Doctor17 = "e4ab1199-1c22-4f82-a516-c3238bc2d0d1";
    public static final String Doctor17PH = "4d0477d7-58da-41a9-a945-e93df8601c5a";
    public static final String ShimmeringBlaze = "061bc566-ec74-4307-9614-ac3a70d2ef38";
    public static final String FifeMiner = "37e5eb63-b9a2-4735-9007-1c77d703daa3";
    public static final String lag_add = "259785a0-20e9-4c63-9286-ac2f93ff528f";
    public static final String Pu_238 = "c95fdfd3-bea7-4255-a44b-d21bc3df95e3";
    public static final String Tankish = "609268ad-5b34-49c2-abba-a9d83229af03";
    public static final String FrizzleFrazzle = "fc4cc2ee-12e8-4097-b26a-1c6cb1b96531";
    public static final String the_NCR = "28ae585f-4431-4491-9ce8-3def6126e3c6";
    public static final String Barnaby99_x = "b04cf173-cff0-4acd-aa19-3d835224b43d";
    public static final String Ma118 = "1121cb7a-8773-491f-8e2b-221290c93d81";
    public static final String Adam29Adam29 = "bbae7bfa-0eba-40ac-a0dd-f3b715e73e61";
    public static final String Alcater = "0b399a4a-8545-45a1-be3d-ece70d7d48e9";
    public static final String ege444 = "42ee978c-442a-4cd8-95b6-29e469b6df10";
    public static final String LePeeperSauvage = "433c2bb7-018c-4d51-acfe-27f907432b5e";

    public static final Set<String> hashes = new HashSet<>();
    static {
        hashes.add("41de5c372b0589bbdb80571e87efa95ea9e34b0d74c6005b8eab495b7afd9994");
        hashes.add("31da6223a100ed348ceb3254ceab67c9cc102cb2a04ac24de0df3ef3479b1036");
    }

    public static final Set<String> contributors = new HashSet<>(Set.of(
            "06ab7c03-55ce-43f8-9d3c-2850e3c652de", //mustang_rudolf
            "5bf069bc-5b46-4179-aafe-35c0a07dee8b", //JMF781
            "ccd9aa1c-26b9-4dde-8f37-b96f8d99de22"  //kakseao
    ));

    /** complete fucking shit */
    public static String smoosh(String s1, String s2, String s3, String s4) {

        Random rand = new Random();
        String s = "";

        byte[] b1 = s1.getBytes();
        byte[] b2 = s2.getBytes();
        byte[] b3 = s3.getBytes();
        byte[] b4 = s4.getBytes();

        if(b1.length == 0 || b2.length == 0 || b3.length == 0 || b4.length == 0) return "";

        s += s1;
        rand.setSeed(b1[0]);
        s += rand.nextInt(0xffffff) + s2;
        rand.setSeed(rand.nextInt(0xffffff) + b2[0]);
        s += rand.nextInt(0xffffff) + s3;
        rand.setSeed(rand.nextInt(0xffffff) + b3[0]);
        s += rand.nextInt(0xffffff) + s4;
        rand.setSeed(rand.nextInt(0xffffff) + b4[0]);
        s += rand.nextInt(0xffffff);
        return getHash(s);
    }

    /** Simple SHA256 call */
    public static String getHash(String inp) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] bytes = sha256.digest(inp.getBytes());
            StringBuilder str = new StringBuilder();
            for(int b : bytes) str.append(Integer.toString((b & 0xFF) + 256, 16).substring(1));
            return str.toString();
        } catch(NoSuchAlgorithmException e) { }
        return "";
    }

    /** Spieler-Pruefung wie im Original: UUID oder Anzeigename. */
    public static boolean is(net.minecraft.world.entity.player.Player player, String uuid, String name) {
        return player.getUUID().toString().equals(uuid) || player.getGameProfile().getName().equals(name);
    }
}
