/**
 * Copyright 2013-2016 Ronald W Hoffman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ScripterRon.BitcoinCore;

import java.io.EOFException;
import java.math.BigInteger;

/**
 * <p>A transaction input has the following format:</p>
 * <pre>
 *   Size           Field               Description
 *   ===            =====               ===========
 *   32 bytes       TxOutHash           Double SHA-256 hash of the transaction containing the output
 *                                      to be used by this input
 *   4 bytes        TxOutIndex          Index of the output within the transaction
 *   VarInt         TxInScriptLength    Script length
 *   Variable       TxInScript          Script
 *   4 bytes        TxInSeqNumber       Input sequence number (irrelevant unless transaction LockTime is
 *                                      non-zero)
 * </pre>
 * <p>All numbers are encoded in little-endian format (least-significant byte to most-significant byte)</p>
 */
public class TransactionInput implements ByteSerializable {

    /** The transaction output connected to this input */
    private final OutPoint outPoint;

    /** Input script */
    private byte[] scriptBytes;

    /** Input sequence number */
    private final int seqNumber;

    /** Parent transaction */
    private final Transaction tx;

    /** Transaction input index */
    private final int txIndex;

    /** Input amount (must be set by the application as needed) */
    private BigInteger value;

    /**
     * Create a transaction input for the specified outpoint.
     *
     * @param       tx                      Parent transaction
     * @param       txIndex                 Transaction input index
     * @param       outPoint                Connected transaction output
     */
    public TransactionInput(Transaction tx, int txIndex, OutPoint outPoint) {
        this(tx, txIndex, outPoint, -1);
    }

    /**
     * Create a transaction input for the specified outpoint.
     *
     * @param       tx                      Parent transaction
     * @param       txIndex                 Transaction input index
     * @param       outPoint                Connected transaction output
     * @param       seqNumber               Transaction input sequence number
     */
    public TransactionInput(Transaction tx, int txIndex, OutPoint outPoint, int seqNumber) {
        this.tx = tx;
        this.txIndex = txIndex;
        this.outPoint = outPoint;
        this.seqNumber = seqNumber;
        this.scriptBytes = new byte[0];
        this.value = BigInteger.ZERO;
    }

    /**
     * Create a transaction input from the encoded byte stream
     *
     * @param       tx                      Parent transaction
     * @param       txIndex                 Transaction input index
     * @param       inBuffer                Input buffer
     * @throws      EOFException            Input stream is too short
     */
    public TransactionInput(Transaction tx, int txIndex, SerializedBuffer inBuffer) throws EOFException {
        this.tx = tx;
        this.txIndex = txIndex;
        this.value = BigInteger.ZERO;
        //
        // Get the transaction output connected to this input
        //
        outPoint = new OutPoint(inBuffer);
        //
        // Get the script
        //
        scriptBytes = inBuffer.getBytes();
        //
        // Get the sequence number
        //
        seqNumber = inBuffer.getInt();
    }

    /**
     * Return the serialized transaction input
     *
     * @param       outBuffer       Output buffer
     * @return                      Output buffer
     */
    @Override
    public SerializedBuffer getBytes(SerializedBuffer outBuffer) {
        outPoint.getBytes(outBuffer)
                .putVarInt(scriptBytes.length)
                .putBytes(scriptBytes)
                .putInt(seqNumber);
        return outBuffer;
    }

    /**
     * Returns the serialized transaction input
     *
     * @return                      Serialized transaction input
     */
    @Override
    public byte[] getBytes() {
        SerializedBuffer buffer = new SerializedBuffer();
        return getBytes(buffer).toByteArray();
    }

    /**
     * Return the transaction containing this input
     *
     * @return                      Parent transaction
     */
    public Transaction getTransaction() {
        return tx;
    }

    /**
     * Return the index of this input within the transaction inputs
     *
     * @return                      Transaction input index
     */
    public int getIndex() {
        return txIndex;
    }

    /**
     * Get the transaction output connected to this input
     *
     * @return                      Transaction output
     */
    public OutPoint getOutPoint() {
        return outPoint;
    }

    /**
     * Return the script bytes for this input
     *
     * @return                      Script bytes
     */
    public byte[] getScriptBytes() {
        return scriptBytes;
    }

    /**
     * Set the script bytes for this input
     *
     * @param       scriptBytes     Script bytes
     */
    public void setScriptBytes(byte[] scriptBytes) {
        this.scriptBytes = scriptBytes;
    }

    /**
     * Return the transaction sequence number
     *
     * @return      Transaction sequence number
     */
    public int getSeqNumber() {
        return seqNumber;
    }

    /**
     * Return the number of coins spent by this input
     *
     * Note: This value must be set by the application since it is not part of the
     * serialized transaction input
     *
     * @return                      Amount
     */
    public BigInteger getValue() {
        return value;
    }

    /**
     * Set the number of coins spent by this input
     *
     * @param       value           Amount
     */
    public void setValue(BigInteger value) {
        this.value = value;
    }

    /**
     * Serialize this input for use in a transaction signature
     *
     * The scriptBytes are replaced by the supplied subScriptBytes.  In addition, the sequence number
     * is set to zero for all hash types other than SIGHASH_ALL.
     *
     * @param       index           Index of the input being signed
     * @param       hashType        Hash type
     * @param       subScriptBytes  Replacement script bytes
     * @param       outBuffer       Output buffer
     */
    public void serializeForSignature(int index, int hashType, byte[] subScriptBytes, SerializedBuffer outBuffer) {
        outPoint.getBytes(outBuffer)
                .putVarInt(subScriptBytes.length)
                .putBytes(subScriptBytes)
                .putInt(hashType==ScriptOpCodes.SIGHASH_ALL||index==txIndex ? seqNumber : 0);
    }
}
