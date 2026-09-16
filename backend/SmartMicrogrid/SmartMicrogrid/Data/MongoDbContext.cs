/*
 * File: MongoDbContext.cs
 * Purpose: Wraps the MongoDB database configured in MongoDbSettings and gives
 *          services access to its collections. Registered as a singleton.
 * Author:  <your name>
 * Created: 2026
 */
using Microsoft.Extensions.Options;
using MongoDB.Bson;
using MongoDB.Driver;
using SmartMicrogrid.Api.Settings;

namespace SmartMicrogrid.Api.Data
{
    public class MongoDbContext
    {
        private readonly IMongoDatabase _database;

        // Opens the configured database from the shared MongoDB client.
        public MongoDbContext(IMongoClient client, IOptions<MongoDbSettings> settings)
        {
            _database = client.GetDatabase(settings.Value.DatabaseName);
        }

        // Returns a typed handle to a collection by name.
        public IMongoCollection<T> GetCollection<T>(string name)
        {
            return _database.GetCollection<T>(name);
        }

        // Sends a ping command to check the database is reachable.
        public Task<BsonDocument> PingAsync(CancellationToken cancellationToken = default)
        {
            return _database.RunCommandAsync<BsonDocument>(new BsonDocument("ping", 1), cancellationToken: cancellationToken);
        }
    }
}
