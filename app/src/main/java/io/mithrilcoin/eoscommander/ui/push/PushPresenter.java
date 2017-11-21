/*
 * Copyright (c) 2017 Mithril coin.
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.mithrilcoin.eoscommander.ui.push;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import javax.inject.Inject;

import io.mithrilcoin.eoscommander.crypto.util.HexUtils;
import io.mithrilcoin.eoscommander.data.EoscDataManager;
import io.mithrilcoin.eoscommander.data.remote.model.api.JsonToBinResponse;
import io.mithrilcoin.eoscommander.ui.base.BasePresenter;
import io.mithrilcoin.eoscommander.ui.base.RxCallbackWrapper;
import io.mithrilcoin.eoscommander.util.StringUtils;
import io.mithrilcoin.eoscommander.util.Utils;
import timber.log.Timber;

/**
 * Created by swapnibble on 2017-11-08.
 */

public class PushPresenter extends BasePresenter<PushMvpView> {

    @Inject
    EoscDataManager mDataManager;

    @Inject
    public PushPresenter(){
    }

    public void onImportFileClicked(){
        getMvpView().openFileManager();
    }


    private String[] getArrayFromCsv(String csv) {
        csv = csv.trim();

        if (StringUtils.isEmpty(csv)) {
            return new String []{""};
        }

        StringTokenizer tokenizer = new StringTokenizer( csv, ",");
        String[] ret = new String[ tokenizer.countTokens() ];

        for ( int i = 0; i < ret.length; i++ ){
            ret[i] = tokenizer.nextToken();
        }

        return ret;
    }

    public void pushMessage( String contract, String action, String message, String scopes, String permissionAccount, String permissionName ){

        getMvpView().showLoading( true );

        addDisposable(
                mDataManager.pushMessage(contract, action, message.replaceAll("\\r|\\n","")
                                , getArrayFromCsv(scopes), new String[]{permissionAccount + "@" + permissionName })
                .subscribeOn( getSchedulerProvider().io())
                .observeOn( getSchedulerProvider().ui())
                .subscribeWith(new RxCallbackWrapper<JsonObject>( this) {
                       @Override
                       public void onNext(JsonObject result) {
                           if (!isViewAttached()) return;

                            getMvpView().showLoading(false);

                            getMvpView().showResult( Utils.prettyPrintJson( result ) );
                       }
                   }
                )
        );
    }

    public void onImportMessageFile( String filePath ) {
        if( StringUtils.isEmpty( filePath )) {
            return;
        }

        BufferedReader reader = null;
        StringBuilder  builder = new StringBuilder();
        char[] buffer = new char[1024];
        int read;

        try {
            reader = new BufferedReader(new FileReader (filePath));
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }

            JSONObject json = new JSONObject(builder.toString()); // Convert text to object
            getMvpView().showContractMessage(json.toString(2)); // Print it with specified indentation
        }
        catch (FileNotFoundException fne) {
            getMvpView().onError( fne.getMessage() );
        }
        catch (IOException e){
            getMvpView().onError( e.getMessage() );
        }
        catch (JSONException jsonE){
            getMvpView().onError( jsonE.getMessage() );
        }
        finally {
            Utils.closeSilently( reader );
        }
    }
}
