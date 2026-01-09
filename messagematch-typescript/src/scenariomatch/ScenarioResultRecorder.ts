import Knex, {} from 'knex'

export class ScenarioResultRecorder {
    knex:Knex.Knex;
    constructor(private dbPath:string) {

        const knex = Knex({
            client: 'sqlite3', // or 'better-sqlite3'
            connection: {
                filename: dbPath,
            },
        });

        this.buildTable(knex)

        this.knex = knex;

    }

    async buildTable(knex: Knex.Knex)
    {
        knex.schema.hasTable('successful_client_tests').then(function (exists:boolean) {
            if (!exists) {
                return knex.schema.createTable('successful_client_tests', function (table) {
                    table.string('client_name', 64);
                    table.string('test_name', 64);
                    table.string('scenario_name', 128);
                    table.string('scenario_hash', 32);
                    table.string('client_version', 32);
                    table.timestamp('runtime');
                });
            }
        });
    }

    public async recordTestResult(clientName:string, testName:string, scenarioName:string, scenarioHash:string, clientVersion:string) {
        return this.knex('successful_client_tests').insert({
            client_name: clientName, test_name: testName, scenario_name: scenarioName,
            scenario_hash: scenarioHash, client_version: clientVersion, runtime: Date.now()
        });
    }
}

//const recorder:ScenarioResultRecorder = new ScenarioResultRecorder("../messagematch/messagematch-junit/messagematch.testresult.db");

//recorder.recordTestResult("fish", "fishTest", "scenario", "123", "0.0.0")

