# Download manager

Is an application to download files from remote servers, FTP/SFTP or HTTP urls.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

- Java 8 and above
- Scala version 2.12.6 & above
- SBT

### Installing

Checkout the project from git

```
git clone https://github.com/gabriel-a/download-manager.git
```

To run

```
sbt run
```

Make sure the application.conf has the right values

| Property name                | Description                                                                 |
|------------------------------|-----------------------------------------------------------------------------|
| final-destination            | The directory where the completed files are saved                           |
| tmp-destination              | The directory that the files are streamed or downloaded to. (Not completed) |
| providers                    | List of providers to connect to                                             |
| - uid                        | Important to be unique, it will be created as a directory on your system    |
| - protocol                   | The protocol: Currently it supports (HTTP, FTP, FTPS)                       |
| - host                       | The Host to connect to                                                      |
| - base-path                  | Host base path                                                              |
| - interval                   | Check the host every (x seconds)                                            |
| - max-concurrent-connections | The max concurrent connections to the host                                  |
| - allowed-ext                | List of comma delimited extensions: (Supports * for everything)             |
| - username                   | The username to the host (Can be left empty)                                |
| - Password                   | The password of the host (Can be left empty)                                |

NB: If using username & password using HTML It's recommended to use https only.

## Running the tests

To test locally simply run

```
sbt test
```

## Deployment

The download manager is developed with Scala & Akka actors

## Built With

* [Scala](https://docs.scala-lang.org/) - The language
* [Akka](https://akka.io/docs/) - Akka actors
* [requests-scala](https://github.com/lihaoyi/requests-scala) - Used to stream files from http to location


## Authors

* **Gabriel Ajabahian** - *Initial work* - [gabriel-a](https://github.com/gabriel-a)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Thanks to Lev that introduced me to this problem
