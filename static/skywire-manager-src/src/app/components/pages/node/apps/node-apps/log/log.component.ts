import { Component, Inject, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { AppsService } from '../../../../../../services/apps.service';
import { LogMessage, Application } from '../../../../../../app.datatypes';
import { MAT_DIALOG_DATA } from '@angular/material';
import { Subscription, of } from 'rxjs';
import { NodeComponent } from '../../../node.component';
import { delay, flatMap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { ErrorsnackbarService } from '../../../../../../services/errorsnackbar.service';

@Component({
  selector: 'app-log',
  templateUrl: './log.component.html',
  styleUrls: ['./log.component.scss'],
})
export class LogComponent implements OnInit, OnDestroy {
  @ViewChild('content') content: ElementRef;

  logMessages: LogMessage[] = [];
  loading = false;

  private shouldShowError = true;
  private subscription: Subscription;

  constructor(
    @Inject(MAT_DIALOG_DATA) private data: Application,
    private appsService: AppsService,
    private translate: TranslateService,
    private errorSnackBar: ErrorsnackbarService,
  ) { }

  ngOnInit() {
    this.loadData(0);
  }

  ngOnDestroy(): void {
    this.removeSubscription();
  }

  private loadData(delayMilliseconds: number) {
    this.removeSubscription();

    this.loading = true;
    this.subscription = of(1).pipe(
      delay(delayMilliseconds),
      flatMap(() => this.appsService.getLogMessages(NodeComponent.getCurrentNodeKey(), this.data.name))
    ).subscribe(
      (log) => this.onLogsReceived(log),
      this.onLogsError.bind(this)
    );
  }

  private removeSubscription() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  private onLogsReceived(logs: string[] = []) {
    this.loading = false;
    logs.forEach(log => {
      const dateStart = log.startsWith('[') ? 0 : -1;
      const dateEnd = dateStart !== -1 ? log.indexOf(']') : -1;

      if (dateStart !== -1 && dateEnd !== -1) {
        this.logMessages.push({
          time: log.substr(dateStart, dateEnd + 1),
          msg: log.substr(dateEnd + 1),
        });
      } else {
        this.logMessages.push({
          time: '',
          msg: log,
        });
      }
    });

    setTimeout(() => {
      (this.content.nativeElement as HTMLElement).scrollTop = (this.content.nativeElement as HTMLElement).scrollHeight;
    });
  }

  private onLogsError() {
    if (this.shouldShowError) {
      this.errorSnackBar.open(this.translate.instant('common.loading-error'));
      this.shouldShowError = false;
    }

    this.loadData(3000);
  }
}
