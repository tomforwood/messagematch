<template>
  <v-container class="fill-height" max-width="900">
    <div>

      <div class="mb-8 text-center">
        MessageMatch compatability
        <v-row>
          <v-autocomplete label="Client Name"
                          id="input-client"
                          v-model="selectedClient"
                          item-title="name"
                          :items="clients"
                          return-object
          ></v-autocomplete>
          <v-select label="Client Version"
                    id="input-version"
                    v-model="selectedVersion"
                    :items="selectedClient.versions"
                    @update:modelValue="onVersionChange">

          </v-select>
        </v-row>
      </div>

      Scenarios with no API test
      <v-row v-for="unsupported in matrix.untestedScenarios">
        {{ unsupported }}

      </v-row>
      Tested Scenarios
      <v-card v-for="api in matrix.apis">
        {{ api.name }}

        <v-table>
          <thead>
          <tr>
            <th>Scenario</th>
            <th v-for="version in api.allVersions">
              {{ version.versionTag }}
            </th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="(scenarioName) in Object.keys(api.scenariosTestedByVersions)">
            <td>{{ scenarioName }}</td>
            <td v-for="version in api.allVersions">
              {{ isTested(api.scenariosTestedByVersions[scenarioName]?.versions, version.versionTag) }}
            </td>
          </tr>
          </tbody>
        </v-table>
      </v-card>
    </div>
  </v-container>
</template>

<script lang="ts">
import {getClients, getMatrix, type ClientDetails, type CompatMatrix } from "../api";
import {defineComponent} from 'vue'

export default defineComponent({
  data() {
    return {
      selectedClient: {} as ClientDetails,
      selectedVersion: "" as string,
      clients: [] as ClientDetails[],
      matrix: {} as CompatMatrix
    }
  },
  mounted() {
    getClients().then(response => {
      this.clients = response.clients;
    })
  },
  methods: {
    onVersionChange() {
      getMatrix(this.selectedClient, this.selectedVersion).then(response => {
        this.matrix = response;
      })
    },
    isTested(tested: any | undefined, version: string) {
      console.log("is tested" , tested, version)
      return tested && tested[version];
    },
  }


})
</script>
