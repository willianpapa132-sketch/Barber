export interface AppointmentSummary {
  id: number;
  dayName: string;
  dayNumber: string;
  startsAt: string;
  endsAt: string;
  barberName: string;
  barberAvatarUrl: string;
  services: string[];
  precoTotal: number;
}

export interface AppointmentDetail extends AppointmentSummary {
  data?: string;
  date: string;
  month?: string;
  details: string;
  status: string;
  statusLabel?: string;
  imageUrl: string;
}

export const nextAppointment: AppointmentSummary | null = {
  id: 1284,
  dayName: 'SAB',
  dayNumber: '26',
  startsAt: '10:00',
  endsAt: '10:45',
  barberName: 'Lucas',
  barberAvatarUrl:
    'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=120&q=80',
  services: ['Corte de Cabelo'],
  precoTotal: 45,
};

export const appointmentDetails: AppointmentDetail[] = [
  {
    id: 1284,
    date: '26 de abr. de 2025',
    dayName: 'SAB',
    dayNumber: '26',
    month: 'ABR',
    startsAt: '10:00',
    endsAt: '10:45',
    barberName: 'Lucas',
    barberAvatarUrl:
      'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=120&q=80',
    services: ['Corte de Cabelo'],
    details: 'Degrade + acabamento',
    status: 'Agendado',
    precoTotal: 45,
    imageUrl:
      'https://images.unsplash.com/photo-1621605815971-fbc98d665033?auto=format&fit=crop&w=160&q=80',
  },
];

export const latestAppointments: AppointmentDetail[] = [
  {
    id: 1175,
    date: '12 de abr. de 2025',
    dayName: 'SAB',
    dayNumber: '12',
    month: 'ABR',
    startsAt: '14:00',
    endsAt: '14:45',
    barberName: 'Lucas',
    barberAvatarUrl:
      'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=120&q=80',
    services: ['Corte de Cabelo'],
    details: 'Degrade + acabamento',
    status: 'Concluido',
    precoTotal: 45,
    imageUrl:
      'https://images.unsplash.com/photo-1621605815971-fbc98d665033?auto=format&fit=crop&w=160&q=80',
  },
  {
    id: 1083,
    date: '28 de mar. de 2025',
    dayName: 'SEX',
    dayNumber: '28',
    month: 'MAR',
    startsAt: '11:00',
    endsAt: '11:30',
    barberName: 'Lucas',
    barberAvatarUrl:
      'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=120&q=80',
    services: ['Barba'],
    details: 'Modelagem',
    status: 'Concluido',
    precoTotal: 35,
    imageUrl:
      'https://images.unsplash.com/photo-1599351431202-1e0f0137899a?auto=format&fit=crop&w=160&q=80',
  },
];
