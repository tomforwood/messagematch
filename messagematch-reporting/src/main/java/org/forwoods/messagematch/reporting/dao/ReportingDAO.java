package org.forwoods.messagematch.reporting.dao;

import org.forwoods.messagematch.reporting.api.ApiCompatibilities;
import org.forwoods.messagematch.reporting.api.ApiTestVersions;
import org.forwoods.messagematch.reporting.api.ApiVersion;
import org.forwoods.messagematch.reporting.api.ClientDetails;
import org.forwoods.messagematch.reporting.api.ClientsList;
import org.forwoods.messagematch.reporting.api.CompatMatrix;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportingDAO {

    private static final String SELECT_CLIENTS = "SELECT DISTINCT client_name, client_version FROM successful_client_tests ORDER BY client_name";
    private final Connection conn;

    public ReportingDAO(String databaseURL) {
        try {
            conn = DriverManager.getConnection(databaseURL);
            if (conn == null) {
                throw new RuntimeException("Unable to connect to test run store");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to connect to test run store", e);
        }
    }

    public ClientsList getClients() {
        try {
            Map<String, List<String>> clients = new HashMap<>();
            final PreparedStatement selectClients;
            selectClients = conn.prepareStatement(SELECT_CLIENTS);
            final ResultSet resultSet = selectClients.executeQuery();
            while (resultSet.next()) {
                clients.computeIfAbsent(resultSet.getString("client_name"),
                        v-> new ArrayList<>())
                        .add(resultSet.getString("client_version"));
            }
            return new ClientsList(clients.entrySet().stream()
                    .map(e->new ClientDetails(e.getKey(), e.getValue()))
                    .toList());
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching clients", e);
        }
    }

    public static final String SELECT_API_COMPATIBILITIES = """
            SELECT c.scenario_name, s.api_name, s.api_version, s.result, s.runtime from successful_client_tests c
            LEFT OUTER JOIN successful_server_tests s using (scenario_hash)
            where c.client_name = ?
            AND c.client_version = ?
            """;

    public CompatMatrix getApiCompatibilities(String clientName, String clientVersion, int cols) {
        try {
            final PreparedStatement selectApiCompatibilities ;
            selectApiCompatibilities = conn.prepareStatement(SELECT_API_COMPATIBILITIES);
            selectApiCompatibilities.setString(1, clientName);
            selectApiCompatibilities.setString(2, clientVersion);
            final ResultSet resultSet = selectApiCompatibilities.executeQuery();
            final List<String> untested = new  ArrayList<>();
            final Map<String, ApiCompatibilities> compatByApi = new HashMap<>();
            final Map<String, List<ApiVersion>> allApiVersions = new HashMap<>();
            while (resultSet.next()) {
                String apiName = resultSet.getString("api_name");
                final String scenarioName = resultSet.getString("scenario_name");
                if (apiName == null) {
                    untested.add(scenarioName);
                }
                else {

                    ApiCompatibilities compat = compatByApi.computeIfAbsent(apiName, newName->new ApiCompatibilities(newName, allApiVersions.computeIfAbsent(newName, n-> new ArrayList<>()), new HashMap<>()));
                    String apiVersion = resultSet.getString("api_version");
                    Instant testTime = Instant.ofEpochMilli(resultSet.getLong("runtime"));
                    Boolean result = resultSet.getBoolean("result");
                    compat.allVersions().add(new ApiVersion(apiVersion, testTime));
                    compat.scenariosTestedByVersions().computeIfAbsent(scenarioName, n->new ApiTestVersions(new HashMap<>()))
                            .versions()
                            .put(apiVersion, result);
                }
            }
            return new CompatMatrix(new ArrayList<>(rebuildVersions(compatByApi.values(), cols)), untested);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Collection<ApiCompatibilities> rebuildVersions(final Collection<ApiCompatibilities> values, final int cols) {
        return values.stream().map(l -> rebuildVersions(l, cols)).toList();
    }

    private ApiCompatibilities rebuildVersions(final ApiCompatibilities l, final int cols) {
        rebuildVersions(l.allVersions(), cols);
        return l;
    }

    private void rebuildVersions(final List<ApiVersion> apiVersions, int cols) {
        Map<String, ApiVersion> latestForVersion = apiVersions.stream().collect(Collectors.toMap(ApiVersion::versionTag,
                v->v,
                (v1,v2)->v1.lastTestTime().isBefore(v2.lastTestTime())?v2:v1));
        apiVersions.retainAll(latestForVersion.values());
        apiVersions.sort(Comparator.comparing(ApiVersion::lastTestTime));
        if (cols < apiVersions.size()) {
            apiVersions.subList(cols, apiVersions.size()).clear();
        }
    }
}
