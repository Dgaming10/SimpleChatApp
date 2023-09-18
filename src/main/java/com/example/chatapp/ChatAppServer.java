package com.example.chatapp;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class ChatAppServer {
    private static final String DATABASE_URL = "jdbc:mysql://127.0.0.1:3306";
    private static final String DATABASE_USER = "root";
    private static final String DATABASE_PASSWORD = "cool_password";
    private static final String SECRET_KEY = "TheBestChatApp!@#";
    private static final String SALT = "THEBESTSAltEver!@#";
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String PADDING_SCHEME = "AES/CBC/PKCS5PADDING";

    private static final Cipher CIPHER;
    private static final SecretKeySpec SECRET_KEY_SPEC;
    private static final IvParameterSpec IV_PARAMETER_SPEC;

    static {
        try {
            SECRET_KEY_SPEC = generateSecretKey(SECRET_KEY);
            CIPHER = Cipher.getInstance(PADDING_SCHEME);
            byte[] initVector = new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
            IV_PARAMETER_SPEC = new IvParameterSpec(initVector);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
    }
    private static SecretKeySpec generateSecretKey(String secretKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), SALT.getBytes(), 65536, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);
    }
    private ChatAppServer() {
    }

    protected static List<String> retrieveMessages(int groupID) {
        List<String> toReturn = new ArrayList<>();
        try (Connection con = getConnection()) {
            String sql = "SELECT * FROM users.chat_app_messages M WHERE M.group_id = ? ORDER BY timestamp";
            try (PreparedStatement preparedStatement = con.prepareStatement(sql)) {
                preparedStatement.setInt(1, groupID);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String message = resultSet.getString(2) + ": " + decryptMessage(resultSet.getString(4));
                        toReturn.add(message);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving messages: " + e.getMessage(), e);
        }
        return toReturn;
    }

    protected static HashMap<String, List<Integer>> getUserGroups(String username) {
        HashMap<String, List<Integer>> toReturn = new HashMap<>();
        try (Connection con = getConnection()) {
            String sql = "SELECT M.groupNAME, M.groupID " +
                    "FROM users.chat_app_groupmembers G " +
                    "JOIN users.chat_app_groups M " +
                    "WHERE M.groupID = G.group_id AND G.username = ?";
            try (PreparedStatement preparedStatement = con.prepareStatement(sql)) {
                preparedStatement.setString(1, username);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String currentGroupName = resultSet.getString(1);
                        if (!toReturn.containsKey(currentGroupName)) {
                            toReturn.put(currentGroupName, new ArrayList<>());
                        }
                        toReturn.get(currentGroupName).add(resultSet.getInt(2));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting user groups: " + e.getMessage(), e);
        }
        return toReturn;
    }

    protected static void sendMessage(String username, int groupID, String content) {
        try (Connection con = getConnection()) {
            String sql = "INSERT INTO users.chat_app_messages(username, group_id, content) VALUES (?, ?, ?)";
            try (PreparedStatement preparedStatement = con.prepareStatement(sql)) {
                preparedStatement.setString(1, username);
                preparedStatement.setInt(2, groupID);
                preparedStatement.setString(3, encryptMessage(content));
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error sending message: " + e.getMessage(), e);
        }
    }

    private static String encryptMessage(String plain) {
        try {
            CIPHER.init(Cipher.ENCRYPT_MODE, SECRET_KEY_SPEC, IV_PARAMETER_SPEC);
            byte[] encrypted = CIPHER.doFinal(plain.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new RuntimeException("Error encrypting message: " + e.getMessage(), e);
        }
    }

    private static String decryptMessage(String encryptedMessage) {
        try {
            CIPHER.init(Cipher.DECRYPT_MODE, SECRET_KEY_SPEC, IV_PARAMETER_SPEC);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedMessage);
            byte[] decryptedBytes = CIPHER.doFinal(encryptedBytes);
            return new String(decryptedBytes);
        } catch (InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new RuntimeException("Error decrypting message: " + e.getMessage(), e);
        }
    }
    protected static void leaveGroup(String username, int groupID) {
        try (Connection con = getConnection()) {
            String sql = "DELETE FROM users.chat_app_groupmembers WHERE username = ? AND group_id = ?";
            try (PreparedStatement preparedStatement = con.prepareStatement(sql)) {
                preparedStatement.setString(1, username);
                preparedStatement.setInt(2, groupID);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error leaving group: " + e.getMessage(), e);
        }
    }

    protected static boolean tryLogin(String username, String password)
    {
        try(Connection con = getConnection()) {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM users.chat_app_users WHERE username = ? AND passwd = ?");
            preparedStatement.setString(1,username);
            preparedStatement.setString(2,md5Hash(password));
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next())
            {
                System.out.println(resultSet.getString(2));
                if(resultSet.getString(2).equals(md5Hash(password)))
                {
                    con.close();
                    return true;
                }
            }
            con.close();
        } catch (SQLException e) {
            System.out.println("PROBLEM WITH CONNECTING TO THE DB");
            throw new RuntimeException(e);
        }
        return false;
    }


    protected static String md5Hash(String toHash)
    {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String string = "";
            for(byte b : md.digest(toHash.getBytes()))
            {
                string += String.format("%02x",b);
            }
            return string;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    protected static boolean registerUser(String username, String password)
    {
        try(Connection con = getConnection()) {
            Statement statement = con.createStatement();
            ResultSet resultSet = statement.executeQuery(String.format("SELECT * FROM users.chat_app_users WHERE username = '%s'",username));
            if(resultSet.next())
            {
                con.close();
                return false;
            }
            statement.executeUpdate(String.format("INSERT INTO users.chat_app_users(username,passwd) VALUES('%s','%s');", username, md5Hash(password)));
            con.close();
            return true;
        } catch (SQLException e) {
            System.out.println("PROBLEM WITH CONNECTING TO THE DB");
            throw new RuntimeException(e);
        }
    }

}
