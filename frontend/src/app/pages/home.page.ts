import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { latestAppointments, nextAppointment } from '../client-data';
import { BottomNav } from '../shared/bottom-nav';

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, BottomNav],
  templateUrl: './home.page.html',
})
export class HomePage {
  protected readonly clientName = 'Willian';
  protected readonly nextAppointment = nextAppointment;
  protected readonly latestAppointments = latestAppointments;
}
