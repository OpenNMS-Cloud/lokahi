# UI Tests

## Prerequisites

Need to have a Lokahi system running locally, with the following:
- User: `admin`
- Password: `admin`
- URL: `https://onmshs.local:1443`

## Running

For now, this test suite can be run locally via `mvn test`.

## TODO

- Minion: the existing tests need to be adjusted to support running a minion.
- Integration with CI: this suite should be setup to run on events of interest (e.g. PRs, environment update).
- Add more tests! :)
