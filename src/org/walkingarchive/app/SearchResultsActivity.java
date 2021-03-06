
package org.walkingarchive.app;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.walkingarchive.app.api.WalkingArchiveApi;
import org.walkingarchive.app.ui.SearchResult;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

/**
 * Renders a list of Search Results
 */
public class SearchResultsActivity extends Activity {
    private static final String TAG = "SearchResultsActivity";
    ListView resultsListView;
    private ArrayAdapter<SearchResult> resultListAdapter;
    private ArrayList<SearchResult> resultListItems;
    private int nextPage;
    
    /**
     * Renders the list. Search Result JSON must be passed as a string extra named resultString 
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_result);
        nextPage = 2;

        String resultString = getIntent().getExtras().getString("resultString");
        JSONArray json;
        try
        {
            json = new JSONArray(resultString);
        }
        catch(JSONException e)
        {
            // Hmm...
            return;
        }

        resultsListView = (ListView) findViewById(R.id.resultsListView);
        
        checkResults(json);
        resultListItems = new ArrayList<SearchResult>();
        for(int i = 0; i < json.length(); i++)
        {
            JSONObject cardJson;
            try
            {
                cardJson = json.getJSONObject(i);
            }
            catch (JSONException e)
            {
                // TODO: Log/whatever this
                continue;
            }
            resultListItems.add(new SearchResult(cardJson));
        }
        
        resultListAdapter = new ArrayAdapter<SearchResult>(this, android.R.layout.simple_list_item_1, resultListItems);
        // Use some arbitrary TextView as a template
        resultsListView.setAdapter(resultListAdapter);
        
        resultsListView.setOnItemClickListener(
            new OnItemClickListener()
            {
                public void onItemClick(AdapterView<?> adapter, View view, int position, long id)
                {
                    SearchResult result = (SearchResult) adapter.getItemAtPosition(position);
                    if(getIntent().hasExtra("forwardResult"))
                    {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("card", result.toJson());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Intent cardViewerIntent = new Intent(SearchResultsActivity.this, CardViewerActivity.class);
                        cardViewerIntent.putExtra("cardJson", result.toJson());
                        SearchResultsActivity.this.startActivity(cardViewerIntent);
                    }
                }
            });
    }
    
    /**
     * Disables the Load More button if there are no more results to retrieve
     * @param json
     */
    private void checkResults(JSONArray json) {
        //if there are less results than the max returned by the api, hide the load button
        if (json.length() < 20 || !getIntent().hasExtra("searchParameters")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Button loadButton = (Button) findViewById(R.id.loadMoreResults);
                    loadButton.setVisibility(View.GONE);
                }
            });
        }
    }
    
    /**
     * Retrieves another set of results from the API cursor and renders them.
     * @param v  The Load More button
     */
    public void loadNextPage(View v) {
        Log.i(TAG, "Next page " + nextPage);
        AsyncTaskCallback resultsCallback = new AsyncTaskCallback()
        {
            public void run(Object o)
            {
                try {
                    checkResults(new JSONArray((String) o));
                    insertIntoList(new JSONArray((String) o));
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        
        WalkingArchiveApi api = new WalkingArchiveApi();
        //get the first page of results

        if(getIntent().hasExtra("searchParameters")) {
            try {
                JSONObject params = new JSONObject(getIntent().getStringExtra("searchParameters"));
                api.getCardByNameManaTypeAsync(params.getString("name"), 
                        params.getString("type"), 
                        params.getString("mana"), 
                        nextPage,
                        resultsCallback);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        nextPage++;
    }
    
    /**
     * Adds a new Card into the results ListView
     * @param json  The Card JSON to add
     */
    private void insertIntoList(JSONArray json) {
        for(int i = 0; i < json.length(); i++){
            JSONObject cardJson;
            try {
                cardJson = json.getJSONObject(i);
            }
            catch (JSONException e) {
                // TODO: Log/whatever this
                continue;
            }
            resultListItems.add(new SearchResult(cardJson));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    resultListAdapter.notifyDataSetChanged();
                }
            });
        }
    }
    
}