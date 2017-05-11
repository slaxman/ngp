/*
 * Copyright © 2013-2016 The NGP Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the NGP software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package ngp.http;


import org.json.simple.JSONStreamAware;

import ngp.Account;
import ngp.Attachment;
import ngp.Constants;
import ngp.PhasingPoll;
import ngp.NgpException;
import ngp.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static ngp.http.JSONResponses.MISSING_TRANSACTION_FULL_HASH;
import static ngp.http.JSONResponses.TOO_MANY_PHASING_VOTES;
import static ngp.http.JSONResponses.UNKNOWN_TRANSACTION_FULL_HASH;

import java.util.ArrayList;
import java.util.List;

public class ApproveTransaction extends CreateTransaction {
    static final ApproveTransaction instance = new ApproveTransaction();

    private ApproveTransaction() {
        super(new APITag[]{APITag.CREATE_TRANSACTION, APITag.PHASING}, "transactionFullHash", "transactionFullHash", "transactionFullHash",
                "revealedSecret", "revealedSecretIsText");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws NgpException {
        String[] phasedTransactionValues = req.getParameterValues("transactionFullHash");

        if (phasedTransactionValues == null || phasedTransactionValues.length == 0) {
            return MISSING_TRANSACTION_FULL_HASH;
        }

        if (phasedTransactionValues.length > Constants.MAX_PHASING_VOTE_TRANSACTIONS) {
            return TOO_MANY_PHASING_VOTES;
        }

        List<byte[]> phasedTransactionFullHashes = new ArrayList<>(phasedTransactionValues.length);
        for (String phasedTransactionValue : phasedTransactionValues) {
            byte[] hash = Convert.parseHexString(phasedTransactionValue);
            PhasingPoll phasingPoll = PhasingPoll.getPoll(Convert.fullHashToId(hash));
            if (phasingPoll == null) {
                return UNKNOWN_TRANSACTION_FULL_HASH;
            }
            phasedTransactionFullHashes.add(hash);
        }

        byte[] secret;
        String secretValue = Convert.emptyToNull(req.getParameter("revealedSecret"));
        if (secretValue != null) {
            boolean isText = "true".equalsIgnoreCase(req.getParameter("revealedSecretIsText"));
            secret = isText ? Convert.toBytes(secretValue) : Convert.parseHexString(secretValue);
        } else {
            String secretText = Convert.emptyToNull(req.getParameter("revealedSecretText"));
            if (secretText != null) {
                secret = Convert.toBytes(secretText);
            } else {
                secret = Convert.EMPTY_BYTE;
            }
        }
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MessagingPhasingVoteCasting(phasedTransactionFullHashes, secret);
        return createTransaction(req, account, attachment);
    }
}
