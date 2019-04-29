import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ITournament, ITournamentForm, Tournament } from 'app/shared/model/tournament.model';
import { TournamentService } from '.';
import { ParticipationService } from 'app/entities/participation';
import { Observable } from 'rxjs';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { JhiAlertService } from 'ng-jhipster';
import { Account, AccountService } from 'app/core';
import { IParticipation, Participation } from 'app/shared/model/participation.model';
import { templateSourceUrl } from '@angular/compiler';
import { Punctuation, IPunctuation } from 'app/shared/model/punctuation.model';
import { PunctuationService, PunctuationTournamentComponent } from 'app/entities/punctuation';
import { filter, map } from 'rxjs/operators';
import { Sponsorship } from 'app/shared/model/sponsorship.model';
// import * as $ from 'jquery';
declare let $: any;

@Component({
    selector: 'jhi-tournament-manage2',
    styles: [
        ' .card{flex-direction: unset} .jh-card{flex-direction: unset} div.finals.round.match {height:0px;top:0} @media only screen and (min-width: 660px) {.card{ display: flex; justify-content: center}}'
    ],
    templateUrl: './tournament-manager.component.html',
    encapsulation: ViewEncapsulation.None
})
export class TournamentManagerComponent implements OnInit {
    tournament: ITournamentForm;

    currentAccount: Account;
    currentDate: Date;

    isSaving: boolean;
    p: IParticipation[];
    i: number;
    l: number;
    participation: Participation;
    punctuations: IPunctuation[];

    constructor(
        protected jhiAlertService: JhiAlertService,
        protected activatedRoute: ActivatedRoute,
        protected accountService: AccountService,
        protected tournamentService: TournamentService,
        protected participationService: ParticipationService,
        protected punctuationService: PunctuationService
    ) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ tournament }) => {
            this.tournament = tournament;
        });
        this.accountService.identity().then(account => {
            this.currentAccount = account;
        });
        this.currentDate = new Date();
        this.p = this.tournament.participations;

        // ....
        // Metodo completo para JQuery. Empieza a partir de aquí:
        if (this.punctuations === undefined) {
            this.punctuations = [];
        }
        // Obtenemos las puntuaciones
        this.punctuationService
            .getPunctuations(this.tournament.id)
            .pipe(
                filter((response: HttpResponse<IPunctuation[]>) => response.ok),
                map((punctuation: HttpResponse<IPunctuation[]>) => punctuation.body)
            )
            .subscribe(value => (this.punctuations = value));
        alert(this.punctuations.filter(x => x.points === 0).length);

        // Vemos en que ronda está el torneo
        let i = 0;
        for (const punctuu of this.punctuations) {
            if (punctuu.round > i) {
                i = punctuu.round;
            }
        }

        // Sacamos las puntuaciones de la última ronda
        const nuevasPuntuaciones = [];
        for (const necesario of this.punctuations) {
            if (necesario.round === i) {
                nuevasPuntuaciones.push(necesario);
            }
        }

        // Ordenamos el array por su ronda y luego por índice
        this.punctuations.sort(function(a, b) {
            const aRound = a.round;
            const bRound = b.round;
            const aIndex = a.index;
            const bIndex = b.index;
            if (aRound === bRound) {
                return aIndex > bIndex ? -1 : aIndex < bIndex ? 1 : 0;
            } else {
                return aRound < bRound ? -1 : 1;
            }
        });

        // Creamos el array a mostrar por JQuery
        const teamsP = [];

        // Usuarios de la ronda
        let userRonda = [];
        let ronda = 0;
        const indexT = 0;
        // Aquí almacenaremos la ronda completa
        let rondaCompleta = [];
        while (ronda <= i) {
            userRonda = [];
            // Sacamos los user de la ronda
            while (this.punctuations[indexT].round === ronda) {
                userRonda.push(this.punctuations[indexT]);
            }
            ronda++;
            const indice2 = 0;
            rondaCompleta = [];
            // Los agrupampos de dos en dos y lo metemos en ronda completa
            while (indice2 < userRonda.length) {
                if (indice2 === userRonda.length - 1) {
                    rondaCompleta.push([this.punctuations[indice2], null]);
                } else {
                    rondaCompleta.push([this.punctuations[indice2], this.punctuations[indice2 + 1]]);
                }
            }
            // Metemos la ronda completa en el array final que muestra JQuery
            teamsP.push([rondaCompleta]);
            ronda++;
        }

        // ANTIGUO METODO
        /*
        if (this.p.length % 2 !== 0) {
            const participationPrueba = null;
            this.p.push(participationPrueba);
        }
        if (this.p.length > 4) {
            while (this.p.length % 8 !== 0) {
                const participationPrueba = null;
                this.p.push(participationPrueba);
            }
        }
        this.l = this.p.length;
        for (this.i = 0; this.i < this.l - 1; this.i++) {
            if (this.i % 2 === 0) {
                if (this.p[this.i] === null) {
                    teamsP.push([null, null]);
                } else if (this.p[this.i + 1] === null) {
                    teamsP.push([this.p[this.i].user.login, null]);
                } else {
                    teamsP.push([this.p[this.i].user.login, this.p[this.i + 1].user.login]);
                }
            }
        }
        */
        const saveData = {
            teams: teamsP,
            results: []
        };
        /* Called whenever bracket is modified
         *
         * data:     changed bracket object in format given to init
         * userData: optional data given when bracket is created.
         */
        function saveFn(data, userData) {
            const json = $.toJSON(data);
            $('#saveOutput').text('POST ' + userData + ' ' + json);
            /* You probably want to do something like this
                jQuery.ajax("rest/"+userData, {contentType: 'application/json',
                                              dataType: 'json',
                                              type: 'post',
                                              data: json})
                */
        }

        $(function() {
            const container = $('.gestionador');
            container.bracket({
                init: saveData,
                userData: 'http://myapi'
            });
            /* You can also inquiry the current data */
            const data = container.bracket('data');

            $('#dataOutput').text($.toJSON(data));
        });
    }

    previousState() {
        window.history.back();
    }

    protected subscribeToSaveResponse(result: Observable<HttpResponse<ITournament>>) {
        result.subscribe((res: HttpResponse<ITournament>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
    }

    protected onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    protected onSaveError() {
        this.isSaving = false;
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
